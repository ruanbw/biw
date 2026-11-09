package com.biw.entity.rbac;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether a role grants anything. Stored by name in {@code sys_role.status}.
 */
@Schema(description = "角色状态")
public enum RoleStatus {

    @Schema(description = "启用，角色上的权限生效")
    ENABLED,

    @Schema(description = "禁用，角色上的权限不生效，已有分配保留")
    DISABLED
}
