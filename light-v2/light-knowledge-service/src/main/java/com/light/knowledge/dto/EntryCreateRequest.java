package com.light.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EntryCreateRequest {
    @NotNull(message = "必须指定模块")
    private Long moduleId;

    @Size(max = 20, message = "类型长度不能超过20个字符")
    @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "类型只能包含字母、数字、下划线和短横线")
    private String type = "note";

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 2000000, message = "笔记内容不能超过2MB")
    private String contentMd;

    private String dataJson;

    @jakarta.validation.constraints.Min(value = 0, message = "状态必须是0、1或2")
    @jakarta.validation.constraints.Max(value = 2, message = "状态必须是0、1或2")
    private Integer status = 0;

    @Size(max = 500, message = "标签总长度不能超过500个字符")
    private String tags;
}
