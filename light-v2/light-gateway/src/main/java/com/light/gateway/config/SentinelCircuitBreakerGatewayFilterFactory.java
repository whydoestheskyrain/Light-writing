package com.light.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

/**
 * Sentinel 版网关熔断过滤器工厂。
 *
 * 背景：Spring Cloud Gateway 5.x（2025.1）把过滤器基类更名为
 * {@link SpringCloudCircuitBreakerFilterFactory}，基类注册名是 "CircuitBreaker"，
 * 官方仅内置 Resilience4J 子类（"SpringCloudCircuitBreakerResilience4J"）；
 * Spring Cloud Alibaba 2025.1.0.0 的适配包只提供了 ReactiveSentinelCircuitBreakerFactory，
 * 没有提供对应的网关过滤器工厂。因此路由配置里写 "SpringCloudCircuitBreaker" 时，
 * 网关找不到工厂并在启动期抛 IllegalArgumentException。
 *
 * 这里继承官方基类、注入 Sentinel 的反应式熔断工厂，并把名称保持为
 * "SpringCloudCircuitBreaker"，使现有路由配置、Sentinel 熔断规则（DegradeRule）
 * 与 fallbackUri 转发全部按原设计生效。
 */
public class SentinelCircuitBreakerGatewayFilterFactory extends SpringCloudCircuitBreakerFilterFactory {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public SentinelCircuitBreakerGatewayFilterFactory(
            ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory,
            ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
        super(reactiveCircuitBreakerFactory, dispatcherHandlerProvider);
    }

    @Override
    public String name() {
        return "SpringCloudCircuitBreaker";
    }

    /** 无 fallbackUri 时的错误透传策略，与官方 Resilience4J 实现保持一致：超时转 504，其余原样抛出 */
    @Override
    protected Mono<Void> handleErrorWithoutFallback(Throwable throwable, boolean timeout) {
        if (throwable instanceof TimeoutException) {
            return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, throwable.getMessage(), throwable));
        }
        return Mono.error(throwable);
    }
}
