package com.biw.entity.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "更新用户请求：字段为 null 表示不修改")
public record UpdateUserRequest(
        @Schema(description = "昵称，最长 64 个字符", example = "爱丽丝")
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Schema(description = "角色：ADMIN 管理员，USER 普通用户", example = "USER",
                allowableValues = {"ADMIN", "USER"})
        @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER")
        String role,

        @Schema(description = "状态：1 启用，0 禁用", example = "1")
        @Min(value = 0, message = "Status must be 0 or 1")
        @Max(value = 1, message = "Status must be 0 or 1")
        Integer status
) {
}
