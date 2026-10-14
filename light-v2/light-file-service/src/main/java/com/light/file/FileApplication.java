package com.light.file;

import com.light.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 文件服务启动类。
 * 职责：图片 / PDF / 代码文件上传，元数据管理。本阶段使用可配置的本地存储目录。
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({GlobalExceptionHandler.class, com.light.common.auth.JwtWebConfiguration.class})
@MapperScan("com.light.file.mapper")
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
