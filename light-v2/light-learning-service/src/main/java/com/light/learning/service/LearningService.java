package com.light.learning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.light.common.auth.CurrentUser;
import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;
import com.light.learning.dto.HeartbeatRequest;
import com.light.learning.entity.LearningDailyRecord;
import com.light.learning.entity.LearningEvent;
import com.light.learning.entity.LearningModuleRecord;
import com.light.learning.mapper.LearningDailyRecordMapper;
import com.light.learning.mapper.LearningEventMapper;
import com.light.learning.mapper.LearningModuleRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习时长服务。
 *
 * 心跳路径：SETNX 幂等去重 → INCR 原子累计到 Redis → 定时任务（默认 60s）GETDEL 刷入聚合表。
 * 查询路径：MySQL 聚合数据 + Redis 中尚未 flush 的实时增量合并返回，
 *          保证心跳上报后页面立即能看到时长，不必等待 flush 周期。
 * Redis 不可用时降级为直接写库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    /** 心跳去重键 TTL：事件最多保留 24 小时 */
    private static final Duration EVENT_TTL = Duration.ofHours(24);
    /** 增量累计键 TTL：给定时 flush 留出窗口，3 天足够 */
    private static final Duration ACCUM_TTL = Duration.ofDays(3);

    private static final String EVENT_PREFIX = "learning:event:";
    private static final String DAILY_PREFIX = "learning:daily:";
    private static final String MODULE_PREFIX = "learning:module:";

    private final LearningEventMapper events;
    private final LearningDailyRecordMapper daily;
    private final LearningModuleRecordMapper modules;
    private final StringRedisTemplate redis;
    private final LearningEventAuditWriter auditWriter;

    public void heartbeat(HeartbeatRequest req) {
        // ① 参数校验
        if (req == null || req.eventId() == null || req.eventId().isBlank()
                || req.seconds() == null || req.seconds() < 1 || req.seconds() > 3600) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "eventId和seconds参数不合法");
        }
        Long uid = CurrentUser.requireId();

        // ② Redis 快路径：SETNX 去重 + INCR 增量累计
        try {
            Boolean first = redis.opsForValue().setIfAbsent(eventKey(uid, req.eventId()), "1", EVENT_TTL);
            if (!Boolean.TRUE.equals(first)) {
                return; // 同一 eventId 已处理过（网络重试），不重复累计
            }
            // 每个心跳携带一个周期的秒数（前端 HEARTBEAT 秒），必须按秒数 INCRBY，而不是固定 +1
            incrWithTtl(dailyKey(uid), req.seconds(), ACCUM_TTL);
            if (req.moduleId() != null) {
                incrWithTtl(moduleKey(uid, req.moduleId()), req.seconds(), ACCUM_TTL);
            }
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            // ③ Redis 不可用降级：直接写库，保证功能可用
            log.warn("Redis 不可用，心跳降级为直接写库: {}", ex.getMessage());
            dbAccumulate(uid, req);
            return;
        }

        // ④ 事件表审计（异步，仅记录不参与计费）
        auditWriter.writeAsync(uid, req.eventId(), req.moduleId(), req.seconds());
    }

    /**
     * 学习汇总。
     *
     * 数据来源 = MySQL 聚合表 + Redis 中尚未被定时任务 flush 的实时增量。
     * 连续打卡语义：今天还没学习不算断签，从昨天起向前数连续有记录的天数。
     */
    public Map<String, Object> summary(String range) {
        Long uid = CurrentUser.requireId();
        LocalDate today = LocalDate.now();
        LocalDate start = "all".equalsIgnoreCase(range)
                ? today.minusYears(10)
                : "week".equalsIgnoreCase(range) ? today.minusDays(6) : today;

        List<LearningDailyRecord> rows = daily.selectList(new LambdaQueryWrapper<LearningDailyRecord>()
                .eq(LearningDailyRecord::getUserId, uid)
                .ge(LearningDailyRecord::getRecordDate, start)
                .le(LearningDailyRecord::getRecordDate, today)
                .orderByAsc(LearningDailyRecord::getRecordDate));

        // Redis 中本用户尚未 flush 的按日增量（date -> seconds）
        Map<LocalDate, Long> pendingDaily = redisAvailable()
                ? scanPendingDaily(uid, start) : Map.of();

        // 合并后的按日视图（用于返回 records 与图表）
        List<LearningDailyRecord> merged = mergeDaily(rows, pendingDaily);

        int dbTotal = rows.stream().mapToInt(x -> x.getSeconds() == null ? 0 : x.getSeconds()).sum();
        long pendingTotal = pendingDaily.values().stream().mapToLong(Long::longValue).sum();
        int total = dbTotal + (int) pendingTotal;

        int todaySeconds = merged.stream()
                .filter(x -> today.equals(x.getRecordDate()))
                .mapToInt(x -> x.getSeconds() == null ? 0 : x.getSeconds()).sum();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("todaySeconds", todaySeconds);
        out.put("totalSeconds", total);
        out.put("streakDays", calcStreak(merged, today));
        out.put("records", merged);
        return out;
    }

    public List<LearningDailyRecord> records() {
        Long uid = CurrentUser.requireId();
        List<LearningDailyRecord> rows = daily.selectList(new LambdaQueryWrapper<LearningDailyRecord>()
                .eq(LearningDailyRecord::getUserId, uid)
                .orderByDesc(LearningDailyRecord::getRecordDate));
        // 叠加上今日 Redis 未 flush 增量，保证刚上报的心跳立刻可见
        if (redisAvailable()) {
            Map<LocalDate, Long> pending = scanPendingDaily(uid, LocalDate.now());
            return mergeDaily(rows, pending).stream()
                    .sorted((a, b) -> b.getRecordDate().compareTo(a.getRecordDate()))
                    .toList();
        }
        return rows;
    }

    /**
     * 按模块聚合学习时长（饼图用）。
     * 返回 [{moduleId, seconds}]，同样合并 Redis 未 flush 的模块增量。
     */
    public List<Map<String, Object>> moduleSummary(String range) {
        Long uid = CurrentUser.requireId();
        LocalDate today = LocalDate.now();
        LocalDate start = "week".equalsIgnoreCase(range) ? today.minusDays(6)
                : "all".equalsIgnoreCase(range) ? today.minusYears(10) : today;

        Map<Long, Long> agg = new LinkedHashMap<>();
        List<LearningModuleRecord> rows = modules.selectList(new LambdaQueryWrapper<LearningModuleRecord>()
                .eq(LearningModuleRecord::getUserId, uid)
                .ge(LearningModuleRecord::getRecordDate, start)
                .le(LearningModuleRecord::getRecordDate, today));
        for (LearningModuleRecord r : rows) {
            agg.merge(r.getModuleId(), (long) r.getSeconds(), Long::sum);
        }
        if (redisAvailable()) {
            scanPendingModule(uid, start).forEach((mid, sec) -> agg.merge(mid, sec, Long::sum));
        }

        List<Map<String, Object>> out = new ArrayList<>();
        agg.forEach((mid, sec) -> {
            if (sec <= 0) return;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("moduleId", String.valueOf(mid));
            item.put("seconds", sec);
            out.add(item);
        });
        out.sort((a, b) -> Long.compare((long) b.get("seconds"), (long) a.get("seconds")));
        return out;
    }

    // ==================== Redis 增量读取（只读，不删除；删除由 flush 负责） ====================

    private boolean redisAvailable() {
        var factory = redis.getConnectionFactory();
        if (factory == null) return false;
        try (var conn = factory.getConnection()) {
            return "PONG".equalsIgnoreCase(conn.ping());
        } catch (Exception ex) {
            return false;
        }
    }

    /** 扫描本用户 daily 增量键，返回 date -> 秒（只保留 date >= start） */
    private Map<LocalDate, Long> scanPendingDaily(Long uid, LocalDate start) {
        Map<LocalDate, Long> out = new LinkedHashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(DAILY_PREFIX + uid + ":*").count(200).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(key -> {
                try {
                    String[] p = key.split(":");
                    LocalDate date = LocalDate.parse(p[3], DATE_FMT);
                    if (date.isBefore(start)) return;
                    Long v = parseLong(redis.opsForValue().get(key));
                    if (v != null && v > 0) out.merge(date, v, Long::sum);
                } catch (Exception ignored) {
                    // 键格式异常忽略，flush 时也会跳过
                }
            });
        }
        return out;
    }

    /** 扫描本用户 module 增量键，返回 moduleId -> 秒（只保留 date >= start） */
    private Map<Long, Long> scanPendingModule(Long uid, LocalDate start) {
        Map<Long, Long> out = new LinkedHashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(MODULE_PREFIX + uid + ":*").count(200).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(key -> {
                try {
                    String[] p = key.split(":");
                    Long moduleId = Long.valueOf(p[3]);
                    LocalDate date = LocalDate.parse(p[4], DATE_FMT);
                    if (date.isBefore(start)) return;
                    Long v = parseLong(redis.opsForValue().get(key));
                    if (v != null && v > 0) out.merge(moduleId, v, Long::sum);
                } catch (Exception ignored) {
                    // 忽略格式异常的键
                }
            });
        }
        return out;
    }

    /** 把 DB 行与 Redis 增量合并为新的按日列表 */
    private List<LearningDailyRecord> mergeDaily(List<LearningDailyRecord> rows, Map<LocalDate, Long> pending) {
        Map<LocalDate, LearningDailyRecord> byDate = new LinkedHashMap<>();
        for (LearningDailyRecord r : rows) byDate.put(r.getRecordDate(), copyOf(r));
        pending.forEach((date, sec) -> byDate
                .computeIfAbsent(date, d -> {
                    LearningDailyRecord r = new LearningDailyRecord();
                    r.setUserId(CurrentUser.requireId());
                    r.setRecordDate(d);
                    r.setSeconds(0);
                    return r;
                })
                .setSeconds(byDate.get(date).getSeconds() + sec.intValue()));
        return new ArrayList<>(byDate.values());
    }

    private LearningDailyRecord copyOf(LearningDailyRecord src) {
        LearningDailyRecord r = new LearningDailyRecord();
        r.setId(src.getId());
        r.setUserId(src.getUserId());
        r.setRecordDate(src.getRecordDate());
        r.setSeconds(src.getSeconds());
        return r;
    }

    /**
     * 连续学习天数。
     * 今天还没有任何记录（DB+Redis 都为 0）时不断签，从昨天开始向前数；
     * 一旦遇到没有记录的日期即终止。
     */
    private int calcStreak(List<LearningDailyRecord> merged, LocalDate today) {
        int streak = 0;
        LocalDate d = today;
        boolean hasToday = merged.stream().anyMatch(x -> today.equals(x.getRecordDate()) && x.getSeconds() > 0);
        if (!hasToday) d = d.minusDays(1);
        var byDate = new java.util.HashSet<LocalDate>();
        merged.forEach(x -> { if (x.getSeconds() > 0) byDate.add(x.getRecordDate()); });
        while (byDate.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 定时把 Redis 增量刷入聚合表（最终一致）。
     * flush 失败时把值回写 Redis，避免数据丢失。
     */
    @Scheduled(fixedDelayString = "${light.learning.flush-interval-ms:60000}")
    public void flushAccumulations() {
        if (!redisAvailable()) return;
        try {
            flushPattern(DAILY_PREFIX + "*");
            flushPattern(MODULE_PREFIX + "*");
        } catch (Exception ex) {
            log.error("学习统计 flush 任务异常", ex);
        }
    }

    private void flushPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(this::flushKey);
        }
    }

    private void flushKey(String key) {
        String value = redis.opsForValue().getAndDelete(key);
        if (value == null || value.isBlank()) return;
        long seconds;
        try {
            seconds = Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return;
        }
        if (seconds <= 0) return;
        try {
            String[] parts = key.split(":");
            if (key.startsWith(DAILY_PREFIX)) {
                Long uid = Long.valueOf(parts[2]);
                LocalDate date = LocalDate.parse(parts[3], DATE_FMT);
                upsertDaily(uid, date, seconds);
            } else if (key.startsWith(MODULE_PREFIX)) {
                Long uid = Long.valueOf(parts[2]);
                Long moduleId = Long.valueOf(parts[3]);
                LocalDate date = LocalDate.parse(parts[4], DATE_FMT);
                upsertModule(uid, moduleId, date, seconds);
            }
        } catch (Exception ex) {
            log.error("flush 失败，回写 Redis 防止丢数据: key={}", key, ex);
            redis.opsForValue().increment(key, seconds);
        }
    }

    private void incrWithTtl(String key, long delta, Duration ttl) {
        Long value = redis.opsForValue().increment(key, delta);
        // 仅在键首次创建（累计值恰好等于本次增量）时设置 TTL，已有键保持原 TTL 不被续期
        if (value != null && value.longValue() == delta) {
            redis.expire(key, ttl);
        }
    }

    private String eventKey(Long uid, String eventId) {
        return EVENT_PREFIX + uid + ":" + eventId;
    }

    private String dailyKey(Long uid) {
        return DAILY_PREFIX + uid + ":" + LocalDate.now().format(DATE_FMT);
    }

    private String moduleKey(Long uid, Long moduleId) {
        return MODULE_PREFIX + uid + ":" + moduleId + ":" + LocalDate.now().format(DATE_FMT);
    }

    /** Redis 不可用时的降级路径：直接操作 DB。 */
    private void dbAccumulate(Long uid, HeartbeatRequest req) {
        if (events.selectCount(new LambdaQueryWrapper<LearningEvent>()
                .eq(LearningEvent::getUserId, uid)
                .eq(LearningEvent::getEventId, req.eventId())) > 0) {
            return;
        }
        LearningEvent event = new LearningEvent();
        event.setUserId(uid);
        event.setEventId(req.eventId());
        event.setModuleId(req.moduleId());
        event.setSeconds(req.seconds());
        event.setEventTime(LocalDateTime.now());
        events.insert(event);

        LocalDate today = LocalDate.now();
        upsertDaily(uid, today, req.seconds());
        if (req.moduleId() != null) {
            upsertModule(uid, req.moduleId(), today, req.seconds());
        }
    }

    private void upsertDaily(Long uid, LocalDate date, long seconds) {
        LearningDailyRecord record = daily.selectOne(new LambdaQueryWrapper<LearningDailyRecord>()
                .eq(LearningDailyRecord::getUserId, uid)
                .eq(LearningDailyRecord::getRecordDate, date));
        if (record == null) {
            record = new LearningDailyRecord();
            record.setUserId(uid);
            record.setRecordDate(date);
            record.setSeconds((int) seconds);
            daily.insert(record);
        } else {
            record.setSeconds(record.getSeconds() + (int) seconds);
            daily.updateById(record);
        }
    }

    private void upsertModule(Long uid, Long moduleId, LocalDate date, long seconds) {
        LearningModuleRecord record = modules.selectOne(new LambdaQueryWrapper<LearningModuleRecord>()
                .eq(LearningModuleRecord::getUserId, uid)
                .eq(LearningModuleRecord::getModuleId, moduleId)
                .eq(LearningModuleRecord::getRecordDate, date));
        if (record == null) {
            record = new LearningModuleRecord();
            record.setUserId(uid);
            record.setModuleId(moduleId);
            record.setRecordDate(date);
            record.setSeconds((int) seconds);
            modules.insert(record);
        } else {
            record.setSeconds(record.getSeconds() + (int) seconds);
            modules.updateById(record);
        }
    }
}
