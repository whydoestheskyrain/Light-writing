package com.light.identity;

import com.light.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 身份服务启动类。
 *
 * 注意：包路径 com.light.identity 与 common 的 com.light.common 不重叠，
 * 这样各服务的 @ComponentScan 默认只扫自己包内的 Bean，避免误扫到其他服务的类。
 * 跨服务共享的代码放 common，通过显式 import 引用。
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import(GlobalExceptionHandler.class)
@MapperScan("com.light.identity.mapper")
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
