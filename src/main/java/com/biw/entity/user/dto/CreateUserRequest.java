package com.biw.entity.user.dto;

import com.biw.entity.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "创建用户请求")
public record CreateUserRequest(
        @Schema(description = "用户名：3-32 个字符，以字母或数字开头，可含字母、数字、下划线、点、短横线；不区分大小写且唯一",
                example = "alice")
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{2,31}$",
                message = "Username may contain letters, digits, '_', '.', '-' and must start with a letter or digit")
        String username,

        @Schema(description = "密码：8 到 64 个字符，UTF-8 编码后不超过 72 字节", example = "P@ssw0rd-example")
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password,

        @Schema(description = "昵称，最长 64 个字符，默认空字符串", example = "爱丽丝")
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Schema(description = "角色编码列表，默认 [USER]；空数组表示不分配角色", example = "[\"USER\"]")
        @Size(max = 20, message = "At most 20 roles per user")
        List<@NotBlank(message = "Role code must not be blank")
             @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$",
                message = "Role code must start with an uppercase letter and contain only A-Z, 0-9 and '_'")
        String> roleCodes,

        @Schema(description = "状态，默认 ENABLED", example = "ENABLED")
        UserStatus status
) {
}
