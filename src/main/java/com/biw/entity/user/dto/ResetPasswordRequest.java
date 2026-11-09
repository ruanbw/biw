package com.biw.entity.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "重置密码请求")
public record ResetPasswordRequest(
        @Schema(description = "新密码：8 到 64 个字符，UTF-8 编码后不超过 72 字节", example = "NewPassw0rd!")
        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String newPassword
) {
}
