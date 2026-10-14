package com.virtuallearn.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条目（模块内的具体内容）
 */
@Data
@TableName("entry")
public class Entry implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long moduleId;

    /** note / algorithm / qa / link */
    private String type;

    private String title;

    private String contentMd;

    /** 扩展字段 JSON */
    private String dataJson;

    /** 0未做 1在做 2已掌握 */
    private Integer status;

    private String tags;

    private LocalDateTime nextReviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
