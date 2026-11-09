package com.biw.entity.user.dto;

import com.biw.entity.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "更新用户请求：字段为 null 表示不修改")
public record UpdateUserRequest(
        @Schema(description = "昵称，最长 64 个字符", example = "爱丽丝")
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Schema(description = "角色编码列表，null = 不修改，空数组 = 清空角色", example = "[\"USER\"]")
        @Size(max = 20, message = "At most 20 roles per user")
        List<@NotBlank(message = "Role code must not be blank")
             @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$",
                message = "Role code must start with an uppercase letter and contain only A-Z, 0-9 and '_'")
        String> roleCodes,

        @Schema(description = "状态", example = "ENABLED")
        UserStatus status
) {
}
