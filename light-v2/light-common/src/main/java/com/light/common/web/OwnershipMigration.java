package com.light.common.web;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** 为旧表添加归属字段，原数据归属保留给迁移账号 id=1。 */
public final class OwnershipMigration {
    private OwnershipMigration() {}
    public static void migrate(DataSource dataSource, String... tables) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String table : tables) {
                boolean exists;
                try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, "user_id")) {
                    exists = columns.next();
                }
                if (!exists) {
                    statement.execute("ALTER TABLE `" + table + "` ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1");
                    statement.execute("CREATE INDEX idx_" + table + "_user ON `" + table + "` (user_id, create_time)");
                }
            }
            if (java.util.Arrays.asList(tables).contains("entry")) {
                statement.execute("ALTER TABLE entry MODIFY tags VARCHAR(500) DEFAULT NULL");
            }
        }
    }
}
