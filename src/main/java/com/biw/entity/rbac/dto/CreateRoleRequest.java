package com.biw.entity.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "创建角色请求")
public record CreateRoleRequest(
        @Schema(description = "角色编码：大写字母开头，可含大写字母、数字、下划线，1-32 个字符；创建后不可修改",
                example = "AUDITOR")
        @NotBlank(message = "Role code must not be blank")
        @Size(max = 32, message = "Role code must not exceed 32 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$",
                message = "Role code must start with an uppercase letter and contain only A-Z, 0-9 and '_'")
        String code,

        @Schema(description = "角色名称，最长 64 个字符", example = "审计员")
        @NotBlank(message = "Role name must not be blank")
        @Size(max = 64, message = "Role name must not exceed 64 characters")
        String name,

        @Schema(description = "角色说明，最长 255 个字符", example = "只能查看用户和角色")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
