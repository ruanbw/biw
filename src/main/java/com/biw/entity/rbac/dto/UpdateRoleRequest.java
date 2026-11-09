package com.biw.entity.rbac.dto;

import com.biw.entity.rbac.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "更新角色请求：字段为 null 表示不修改；角色编码不可修改")
public record UpdateRoleRequest(
        @Schema(description = "角色名称，最长 64 个字符", example = "审计员")
        @Size(min = 1, max = 64, message = "Role name must be between 1 and 64 characters")
        String name,

        @Schema(description = "角色说明，最长 255 个字符", example = "只能查看用户和角色")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @Schema(description = "角色状态；内置角色不能禁用", example = "ENABLED")
        RoleStatus status
) {
}
