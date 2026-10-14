-- 创建数据库
CREATE DATABASE IF NOT EXISTS virtual_learn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE virtual_learn;

-- 模块表
DROP TABLE IF EXISTS `module`;
CREATE TABLE `module` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '模块名',
    `icon`        VARCHAR(20)  DEFAULT NULL COMMENT 'emoji 图标',
    `color`       VARCHAR(20)  DEFAULT NULL COMMENT '主题色',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片路径',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '简介',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='学习模块';

-- 条目表
DROP TABLE IF EXISTS `entry`;
CREATE TABLE `entry` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module_id`        BIGINT       NOT NULL COMMENT '所属模块',
    `type`             VARCHAR(20)  NOT NULL COMMENT '类型 note/algorithm/qa/link',
    `title`            VARCHAR(200) NOT NULL COMMENT '标题',
    `content_md`       LONGTEXT COMMENT 'Markdown 正文',
    `data_json`        JSON COMMENT '扩展字段',
    `status`           TINYINT      DEFAULT 0 COMMENT '0未做 1在做 2已掌握',
    `tags`             VARCHAR(200) DEFAULT NULL COMMENT '逗号分隔',
    `next_review_time` DATETIME     DEFAULT NULL COMMENT '下次复习时间',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='条目';

-- 示例数据
INSERT INTO `module` (`name`, `icon`, `color`, `description`, `sort_order`)
VALUES ('算法刷题', '🧮', '#EA4C89', 'LeetCode 算法题练习', 1),
       ('八股文', '📖', '#4F8CFF', 'Java 面试八股文整理', 2),
       ('学习笔记', '📝', '#52C41A', '日常学习笔记', 3);

INSERT INTO `entry` (`module_id`, `type`, `title`, `content_md`, `data_json`, `status`, `tags`)
VALUES (1, 'algorithm', '两数之和',
        '# 两数之和\n\n给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出和为目标值的那两个整数。',
        '{"leetcodeId":"1","difficulty":"easy","complexity":"O(n)"}', 2, '数组,哈希表'),
       (2, 'qa', 'HashMap 底层原理',
        '# HashMap 底层原理\n\nJDK 1.8 采用 数组 + 链表 + 红黑树 实现。',
        NULL, 1, 'Java,集合'),
       (3, 'note', 'Spring Boot 自动装配',
        '# Spring Boot 自动装配\n\n通过 `@EnableAutoConfiguration` 注解加载 `META-INF/spring.factories`。',
        NULL, 0, 'SpringBoot');
