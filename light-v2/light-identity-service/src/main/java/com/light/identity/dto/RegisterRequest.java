package com.light.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须为3到50个字符")
    @Pattern(regexp = "[a-zA-Z0-9_\\-]+", message = "用户名只能包含字母、数字、下划线和短横线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须为6到72个字符")
    private String password;

    @Size(max = 80, message = "昵称不能超过80个字符")
    private String nickname;
}
