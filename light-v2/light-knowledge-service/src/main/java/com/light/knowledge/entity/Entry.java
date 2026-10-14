package com.light.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("entry")
public class Entry {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long moduleId;
    private String type;
    private String title;
    private String contentMd;
    private String dataJson;
    private Integer status;
    private String tags;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
