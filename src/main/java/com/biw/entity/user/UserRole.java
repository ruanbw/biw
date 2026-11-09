package com.biw.entity.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What an account is allowed to do. Stored by name in {@code sys_user.role}.
 */
@Schema(description = "用户角色")
public enum UserRole {

    @Schema(description = "管理员，可以管理用户")
    ADMIN,

    @Schema(description = "普通用户，只能读取自己的信息")
    USER
}
