package com.light.knowledge.config;

import com.light.common.web.OwnershipMigration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class OwnershipInitializer {
    @Bean
    ApplicationRunner migrateOwnership(DataSource source) {
        return args -> OwnershipMigration.migrate(source, "module", "entry");
    }
}
