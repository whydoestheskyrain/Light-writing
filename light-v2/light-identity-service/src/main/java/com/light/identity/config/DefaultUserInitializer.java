package com.light.identity.config;

import com.light.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DefaultUserInitializer {
    private final IdentityService identityService;
    @Bean
    ApplicationRunner createDefaultUser() {
        return args -> identityService.ensureDefaultUser();
    }
}
