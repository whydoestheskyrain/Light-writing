package com.light.knowledge;

import com.light.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 知识服务启动类。
 * 职责：模块、笔记、算法题、八股、标签管理。
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({GlobalExceptionHandler.class, com.light.common.auth.JwtWebConfiguration.class})
@MapperScan("com.light.knowledge.mapper")
public class KnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
