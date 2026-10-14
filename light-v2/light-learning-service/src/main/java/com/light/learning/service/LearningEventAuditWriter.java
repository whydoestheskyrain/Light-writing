package com.light.learning.service;

import com.light.learning.entity.LearningEvent;
import com.light.learning.mapper.LearningEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 学习事件审计写入器。
 *
 * Redis 快路径下心跳只做增量累计，事件表仅作为审计/去重兜底记录，
 * 用 @Async 异步落库，失败不影响计费正确性（Redis SETNX 已保证去重）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningEventAuditWriter {
    private final LearningEventMapper events;

    @Async
    public void writeAsync(Long userId, String eventId, Long moduleId, Integer seconds) {
        try {
            LearningEvent event = new LearningEvent();
            event.setUserId(userId);
            event.setEventId(eventId);
            event.setModuleId(moduleId);
            event.setSeconds(seconds);
            event.setEventTime(LocalDateTime.now());
            events.insert(event);
        } catch (Exception ex) {
            log.warn("学习事件审计写入失败 userId={} eventId={}: {}",
                    userId, eventId, ex.getMessage());
        }
    }
}
