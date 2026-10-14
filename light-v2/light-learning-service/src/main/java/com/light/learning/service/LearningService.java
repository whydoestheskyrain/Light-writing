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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习时长服务。
 *
 * 心跳优化（v2.1 起）：
 * - 去重 + 累计全部走 Redis（SETNX 幂等、INCR 原子累加），QPS 压力从 MySQL 移到 Redis
 * - 定时任务（默认 60s）用 GETDEL 原子取出增量，最终一致刷入聚合表
 * - Redis 不可用时自动降级为直接写库，功能不受影响
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
            incrWithTtl(dailyKey(uid), ACCUM_TTL);
            if (req.moduleId() != null) {
                incrWithTtl(moduleKey(uid, req.moduleId()), ACCUM_TTL);
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

    public Map<String, Object> summary(String range) {
        Long uid = CurrentUser.requireId();
        LocalDate end = LocalDate.now();
        LocalDate start = "all".equalsIgnoreCase(range)
                ? end.minusYears(10)
                : "week".equalsIgnoreCase(range) ? end.minusDays(6) : end;
        List<LearningDailyRecord> rows = daily.selectList(new LambdaQueryWrapper<LearningDailyRecord>()
                .eq(LearningDailyRecord::getUserId, uid)
                .ge(LearningDailyRecord::getRecordDate, start)
                .le(LearningDailyRecord::getRecordDate, end)
                .orderByAsc(LearningDailyRecord::getRecordDate));
        int total = rows.stream().mapToInt(x -> x.getSeconds() == null ? 0 : x.getSeconds()).sum();

        int streak = 0;
        for (LocalDate d = end; !d.isBefore(start); d = d.minusDays(1)) {
            final LocalDate day = d;
            if (rows.stream().noneMatch(x -> day.equals(x.getRecordDate()) && x.getSeconds() > 0)) break;
            streak++;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("todaySeconds", rows.stream()
                .filter(x -> end.equals(x.getRecordDate()))
                .mapToInt(x -> x.getSeconds()).sum());
        out.put("totalSeconds", total);
        out.put("streakDays", streak);
        out.put("records", rows);
        return out;
    }

    public List<LearningDailyRecord> records() {
        return daily.selectList(new LambdaQueryWrapper<LearningDailyRecord>()
                .eq(LearningDailyRecord::getUserId, CurrentUser.requireId())
                .orderByDesc(LearningDailyRecord::getRecordDate));
    }

    /**
     * 定时把 Redis 增量刷入聚合表（最终一致）。
     * flush 失败时把值回写 Redis，避免数据丢失。
     */
    @Scheduled(fixedDelayString = "${light.learning.flush-interval-ms:60000}")
    public void flushAccumulations() {
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

    private void incrWithTtl(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        if (value != null && value.longValue() == 1L) {
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

    /** Redis 不可用时的降级路径：直接操作 DB（原逻辑）。 */
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
