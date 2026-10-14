package com.light.learning.config;

import com.light.common.auth.JwtWebConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
@MapperScan("com.light.learning.mapper")
@Import(JwtWebConfiguration.class)
public class LearningConfig {}
