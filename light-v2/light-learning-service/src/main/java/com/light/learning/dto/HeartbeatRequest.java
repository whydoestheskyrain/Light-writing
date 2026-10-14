package com.light.learning.dto;
public record HeartbeatRequest(String eventId, Integer seconds, Long moduleId) {}
