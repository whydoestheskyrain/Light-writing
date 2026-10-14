package com.virtuallearn.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学习模块（卡片）
 */
@Data
@TableName("module")
public class Module implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String icon;

    private String color;

    /** 封面图片相对路径，如 /uploads/xxx.jpg；为空时使用渐变封面 */
    private String coverImage;

    private String description;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
