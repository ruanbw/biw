package com.biw.entity.user.dto;

import com.biw.entity.user.UserRole;
import com.biw.entity.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "更新用户请求：字段为 null 表示不修改")
public record UpdateUserRequest(
        @Schema(description = "昵称，最长 64 个字符", example = "爱丽丝")
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Schema(description = "角色", example = "USER")
        UserRole role,

        @Schema(description = "状态", example = "ENABLED")
        UserStatus status
) {
}
