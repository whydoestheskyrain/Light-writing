package com.virtuallearn.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.virtuallearn.knowledge.mapper")
public class KnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  knowledge-service 启动成功: http://localhost:9001");
        System.out.println("========================================\n");
    }
}
