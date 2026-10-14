package com.light.identity.controller;

import com.light.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 临时连通性测试接口。
 * 2.1.0 实现真正的 user API 后删除此 controller。
 *
 * 验证目标：
 *   GET http://localhost:8080/api/identity/ping
 *   -> 网关路由 /api/identity/** 到 identity 服务（8101）
 *   -> identity 服务返回 pong
 */
@RestController
@RequestMapping("/api/identity")
public class PingController {

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.success(Map.of(
                "service", "identity",
                "pong", Instant.now()
        ));
    }
}
