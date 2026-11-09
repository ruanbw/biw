package com.biw.entity.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "设置角色权限请求；提交的列表会整体替换角色原有权限")
public record AssignPermissionsRequest(
        @Schema(description = "权限编码列表；空数组表示撤销角色上的全部权限",
                example = "[\"user:read\", \"role:read\"]")
        @NotNull(message = "permissionCodes must not be null")
        @Size(max = 200, message = "At most 200 permissions per role")
        List<@NotBlank(message = "permission code must not be blank")
             @Size(max = 64, message = "Permission code must not exceed 64 characters")
             String> permissionCodes
) {
}
