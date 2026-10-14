package com.light.learning.service;

import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.learning.dto.HeartbeatRequest;
import com.light.learning.entity.LearningDailyRecord;
import com.light.learning.entity.LearningEvent;
import com.light.learning.entity.LearningModuleRecord;
import com.light.learning.mapper.LearningDailyRecordMapper;
import com.light.learning.mapper.LearningEventMapper;
import com.light.learning.mapper.LearningModuleRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock LearningEventMapper events;
    @Mock LearningDailyRecordMapper daily;
    @Mock LearningModuleRecordMapper modules;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock LearningEventAuditWriter auditWriter;

    @InjectMocks LearningService service;

    @BeforeEach
    void setUp() {
        // lenient：参数校验失败的测试不会触及 Redis，避免 UnnecessaryStubbing
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        CurrentUser.set(new JwtService.UserPrincipal(1L, "light", "USER"));
        // 预注册实体的 TableInfo，否则 LambdaQueryWrapper 抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, com.light.learning.entity.LearningEvent.class);
        TableInfoHelper.initTableInfo(assistant, LearningDailyRecord.class);
        TableInfoHelper.initTableInfo(assistant, LearningModuleRecord.class);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private HeartbeatRequest heartbeat() {
        return new HeartbeatRequest("event-1", 60, 10L);
    }

    @Test
    void heartbeat_blankEventId_shouldThrowBadRequest() {
        assertThrows(BusinessException.class,
                () -> service.heartbeat(new HeartbeatRequest("", 60, 10L)));
    }

    @Test
    void heartbeat_invalidSeconds_shouldThrowBadRequest() {
        assertThrows(BusinessException.class,
                () -> service.heartbeat(new HeartbeatRequest("e1", 0, 10L)));
        assertThrows(BusinessException.class,
                () -> service.heartbeat(new HeartbeatRequest("e1", 3601, 10L)));
    }

    @Test
    void heartbeat_firstTime_shouldAccumulateAndAudit() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.heartbeat(heartbeat());

        // 去重键 + 每日 + 模块，共 3 次增量操作
        verify(valueOps).setIfAbsent(anyString(), eq("1"), any(Duration.class));
        verify(valueOps, org.mockito.Mockito.times(2)).increment(anyString());
        verify(auditWriter).writeAsync(1L, "event-1", 10L, 60);
    }

    @Test
    void heartbeat_duplicateEvent_shouldSkip() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        service.heartbeat(heartbeat());

        verify(valueOps, never()).increment(anyString());
        verify(auditWriter, never()).writeAsync(any(), any(), any(), any());
    }

    @Test
    void heartbeat_redisDown_shouldFallbackToDb() {
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(valueOps).setIfAbsent(anyString(), anyString(), any(Duration.class));

        service.heartbeat(heartbeat());

        // 降级路径：事件表 + 每日聚合表直接写库
        verify(events).insert(any(LearningEvent.class));
        verify(daily).insert(any(LearningDailyRecord.class));
        verify(auditWriter, never()).writeAsync(any(), any(), any(), any());
    }

    @Test
    void heartbeat_withoutModule_shouldSkipModuleAccumulation() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.heartbeat(new HeartbeatRequest("event-2", 60, null));

        // 只有去重键 + 每日，共 1 次 increment（不产生模块 key）
        verify(valueOps, org.mockito.Mockito.times(1)).increment(anyString());
    }

    @Test
    void flushKey_daily_shouldUpsertDailyRecord() {
        when(valueOps.getAndDelete("learning:daily:1:20261014")).thenReturn("60");
        when(daily.selectOne(any())).thenReturn(null);

        ReflectionTestUtils.invokeMethod(service, "flushKey", "learning:daily:1:20261014");

        verify(daily).insert(any(LearningDailyRecord.class));
    }

    @Test
    void flushKey_module_shouldUpsertModuleRecord() {
        when(valueOps.getAndDelete("learning:module:1:10:20261014")).thenReturn("120");
        when(modules.selectOne(any())).thenReturn(null);

        ReflectionTestUtils.invokeMethod(service, "flushKey", "learning:module:1:10:20261014");

        verify(modules).insert(any(LearningModuleRecord.class));
    }

    @Test
    void flushKey_dbFailure_shouldWriteBackToRedis() {
        when(valueOps.getAndDelete("learning:daily:1:20261014")).thenReturn("60");
        when(daily.selectOne(any())).thenThrow(new RuntimeException("DB down"));

        ReflectionTestUtils.invokeMethod(service, "flushKey", "learning:daily:1:20261014");

        verify(valueOps).increment("learning:daily:1:20261014", 60);
    }
}
