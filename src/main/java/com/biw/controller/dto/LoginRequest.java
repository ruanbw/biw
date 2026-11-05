package com.biw.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过 64 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 128, message = "密码长度需在 6-128 个字符之间")
        String password
) {
}
