package com.light.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel 规则加载。
 *
 * - 网关流控：按路由 ID 限制 QPS，超限走 scg fallback（429 + JSON）
 * - 熔断降级：按 CircuitBreaker 名称配置异常比例阈值，触发后进入熔断窗口，
 *   请求直接走 fallbackUri，不再转发下游
 *
 * 开发阶段用代码加载规则；生产可换成 Nacos 数据源（sentinel-datasource-nacos）动态下发。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /** 各路由 QPS 上限（开发环境宽松阈值，按压测结果调整） */
    private static final int QPS_IDENTITY = 50;
    private static final int QPS_KNOWLEDGE = 200;
    private static final int QPS_FILE = 50;
    private static final int QPS_LEARNING = 100;

    /** 熔断阈值：异常比例 50%、最少请求数 5、熔断窗口 10 秒 */
    private static final double DEGRADE_RATIO = 0.5;
    private static final int MIN_REQUEST_AMOUNT = 5;
    private static final int TIME_WINDOW_SECONDS = 10;

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

    @PostConstruct
    public void init() {
        loadGatewayRules();
        loadDegradeRules();
        log.info("Sentinel 网关限流与熔断规则已加载");
    }

    private void loadGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("identity-service").setCount(QPS_IDENTITY));
        rules.add(new GatewayFlowRule("knowledge-service").setCount(QPS_KNOWLEDGE));
        rules.add(new GatewayFlowRule("file-service").setCount(QPS_FILE));
        rules.add(new GatewayFlowRule("learning-service").setCount(QPS_LEARNING));
        GatewayRuleManager.loadRules(rules);
    }

    private void loadDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        for (String cbName : List.of(
                "identityServiceCB", "knowledgeServiceCB", "fileServiceCB", "learningServiceCB")) {
            DegradeRule rule = new DegradeRule(cbName)
                    .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                    .setCount(DEGRADE_RATIO)
                    .setTimeWindow(TIME_WINDOW_SECONDS)
                    .setMinRequestAmount(MIN_REQUEST_AMOUNT);
            rules.add(rule);
        }
        DegradeRuleManager.loadRules(rules);
    }
}
