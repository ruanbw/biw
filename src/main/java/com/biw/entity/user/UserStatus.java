package com.biw.entity.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether an account may log in. Stored by name in {@code sys_user.status}.
 */
@Schema(description = "用户状态")
public enum UserStatus {

    @Schema(description = "启用，可以登录")
    ENABLED,

    @Schema(description = "禁用，不能登录，已有会话由管理接口主动下线")
    DISABLED
}
