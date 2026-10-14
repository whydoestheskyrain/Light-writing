package com.light.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 熔断/降级统一响应端点。
 * CircuitBreaker 过滤器的 fallbackUri 用 forward: 转发到本 Controller。
 */
@RestController
public class FallbackController {

    @GetMapping("/fallback/{service}")
    public Map<String, Object> fallback(@PathVariable String service) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("msg", "服务繁忙或暂不可用，请稍后重试");
        body.put("service", service);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
