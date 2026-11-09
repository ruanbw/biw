package com.biw.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.biw.common.api.Result;
import com.biw.config.OpenApiConfig;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.SysPermission;
import com.biw.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "权限目录", description = "代码定义的权限码，只读")
@SecurityRequirement(name = OpenApiConfig.TOKEN_SCHEME)
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "查询权限目录",
            description = "按权限码升序返回全部权限。权限码由 PermissionCodes 定义、由 sql/init.sql 写入，"
                    + "本项目不提供权限的写接口。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "权限目录"),
            @ApiResponse(responseCode = "403", description = "缺少 permission:read 权限")
    })
    @SaCheckPermission(PermissionCodes.PERMISSION_READ)
    @GetMapping
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionService.listAll());
    }
}
