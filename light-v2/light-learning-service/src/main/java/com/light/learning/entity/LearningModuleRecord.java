package com.light.learning.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
@Data @TableName("learning_module_record")
public class LearningModuleRecord { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long userId; private Long moduleId; private LocalDate recordDate; private Integer seconds; }
