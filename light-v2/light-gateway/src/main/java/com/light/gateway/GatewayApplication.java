package com.light.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类。
 *
 * 网关职责（按演进顺序）：
 * 1. 路由分发：把 /api/{service}/** 转发到对应后端服务（当前阶段）
 * 2. 鉴权：解析 JWT，把用户身份注入到下游请求头（2.1.0 阶段）
 * 3. 限流：基于 Redis 令牌桶（2.4.0 阶段）
 * 4. 熔断：服务异常时返回降级响应（2.4.0 阶段）
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
