CREATE TABLE IF NOT EXISTS `module` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `icon` VARCHAR(20) DEFAULT NULL,
    `color` VARCHAR(20) DEFAULT NULL,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_module_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `module_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL DEFAULT 'note',
    `title` VARCHAR(200) NOT NULL,
    `content_md` LONGTEXT,
    `data_json` JSON,
    `status` TINYINT NOT NULL DEFAULT 0,
    `tags` VARCHAR(200) DEFAULT NULL,
    `next_review_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_entry_module_id` (`module_id`),
    KEY `idx_entry_status` (`status`),
    CONSTRAINT `fk_entry_module` FOREIGN KEY (`module_id`) REFERENCES `module` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
