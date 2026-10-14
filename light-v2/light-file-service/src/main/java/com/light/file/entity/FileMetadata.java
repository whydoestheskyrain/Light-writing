package com.light.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_metadata")
public class FileMetadata {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long entryId;
    private Long userId;
    private String originalName;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private LocalDateTime createTime;
}
