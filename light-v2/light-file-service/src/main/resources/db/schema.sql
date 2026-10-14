CREATE TABLE IF NOT EXISTS `file_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `entry_id` BIGINT DEFAULT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `object_key` VARCHAR(320) NOT NULL,
    `content_type` VARCHAR(150) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `sha256` CHAR(64) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_object_key` (`object_key`),
    KEY `idx_file_entry_id` (`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
