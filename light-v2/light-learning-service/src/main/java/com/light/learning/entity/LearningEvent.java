package com.light.learning.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("learning_event")
public class LearningEvent { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long userId; private String eventId; private Long moduleId; private Integer seconds; private LocalDateTime eventTime; }
