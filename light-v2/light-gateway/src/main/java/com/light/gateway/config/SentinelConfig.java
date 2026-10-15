package com.light.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * Sentinel 接线配置。
 *
 * 规则来源已外置到 Nacos（见 application.yml 的 spring.cloud.sentinel.datasource）：
 * - light-gateway-gw-flow.json：按路由 ID 的 QPS 流控规则（SENTINEL_GROUP）
 * - light-gateway-degrade.json：按 CircuitBreaker 名称的异常比例熔断规则
 * 在 Nacos 控制台修改并发布后秒级生效，无需重启。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /**
     * 注册 Sentinel 版网关熔断过滤器，名称保持 "SpringCloudCircuitBreaker"，
     * 与 application.yml 各路由的 filters 配置对应。Gateway 5.x 不再自带该名称的工厂。
     */
    @Bean
    public SentinelCircuitBreakerGatewayFilterFactory sentinelCircuitBreakerGatewayFilterFactory(
            ReactiveCircuitBreakerFactory<?, ?> reactiveCircuitBreakerFactory,
            ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
        return new SentinelCircuitBreakerGatewayFilterFactory(reactiveCircuitBreakerFactory, dispatcherHandlerProvider);
    }
}
