package com.light.common.auth;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JwtService.class)
public class JwtWebConfiguration {
    @Bean
    public FilterRegistrationBean<JwtRequestFilter> jwtRequestFilter(JwtService jwtService) {
        FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtRequestFilter(jwtService));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(0);
        return registration;
    }
}
