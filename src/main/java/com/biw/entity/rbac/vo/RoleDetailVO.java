package com.biw.entity.rbac.vo;

import com.biw.entity.rbac.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "角色详情")
public record RoleDetailVO(
        @Schema(description = "角色信息") SysRole role,
        @Schema(description = "角色拥有的权限编码；ADMIN 返回 [\"*\"] 表示全部权限",
                example = "[\"user:read\", \"role:read\"]")
        List<String> permissionCodes,
        @Schema(description = "当前未删除且持有该角色的用户数（含禁用用户）", example = "3")
        long userCount
) {
}
