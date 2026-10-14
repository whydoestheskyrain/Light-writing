package com.light.learning;

import com.light.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 学习服务启动类。
 * 职责：学习时长记录、复习计划、学习统计。
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({GlobalExceptionHandler.class, com.light.common.auth.JwtWebConfiguration.class})
public class LearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningApplication.class, args);
    }
}
