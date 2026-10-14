package com.light.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleCreateRequest {
    @NotBlank(message = "模块名称不能为空")
    @Size(max = 50, message = "模块名称长度不能超过50个字符")
    private String name;
    @Size(max = 20, message = "图标长度不能超过20个字符") private String icon;
    @Size(max = 20, message = "颜色长度不能超过20个字符") private String color;
    @Size(max = 255, message = "封面地址长度不能超过255个字符") private String coverImage;
    @Size(max = 200, message = "描述长度不能超过200个字符") private String description;
    private Integer sortOrder = 0;
}
