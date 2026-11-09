package com.biw.entity.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名，不区分大小写", example = "admin")
        @NotBlank(message = "Username must not be blank")
        @Size(max = 64, message = "Username must not exceed 64 characters")
        String username,

        @Schema(description = "密码，UTF-8 编码后不超过 72 字节", example = "Passw0rd!")
        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
        String password
) {
}
