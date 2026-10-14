package com.light.learning.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
@Data @TableName("learning_daily_record")
public class LearningDailyRecord { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long userId; private LocalDate recordDate; private Integer seconds; }
