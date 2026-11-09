package com.biw.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.biw.common.api.PageResult;
import com.biw.common.api.Result;
import com.biw.config.OpenApiConfig;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.SysRole;
import com.biw.entity.rbac.dto.AssignPermissionsRequest;
import com.biw.entity.rbac.dto.CreateRoleRequest;
import com.biw.entity.rbac.dto.UpdateRoleRequest;
import com.biw.entity.rbac.vo.RoleDetailVO;
import com.biw.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "角色管理", description = "角色的增删改查与权限分配")
@SecurityRequirement(name = OpenApiConfig.TOKEN_SCHEME)
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "分页查询角色",
            description = "按创建时间倒序返回；keyword 同时匹配角色编码和角色名称。")
    @ApiResponse(responseCode = "200", description = "分页结果")
    @SaCheckPermission(PermissionCodes.ROLE_READ)
    @GetMapping
    public Result<PageResult<SysRole>> list(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，1-100", example = "20")
            @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "关键字，模糊匹配角色编码或名称", example = "AUDIT")
            @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.listPage(page, size, keyword));
    }

    @Operation(summary = "按 ID 查询角色", description = "同时返回角色拥有的权限编码；ADMIN 返回 [\"*\"]。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "角色详情"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @SaCheckPermission(PermissionCodes.ROLE_READ)
    @GetMapping("/{id}")
    public Result<RoleDetailVO> get(
            @Parameter(description = "角色 ID", example = "1") @PathVariable Long id) {
        return Result.ok(roleService.get(id));
    }

    @Operation(summary = "创建角色", description = "角色编码必须为大写且唯一；新角色默认启用，初始没有任何权限。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功，返回新角色"),
            @ApiResponse(responseCode = "400", description = "角色编码或名称不合法"),
            @ApiResponse(responseCode = "409", description = "角色编码已存在")
    })
    @SaCheckPermission(PermissionCodes.ROLE_WRITE)
    @PostMapping
    public Result<SysRole> create(@Valid @RequestBody CreateRoleRequest request) {
        return Result.ok(roleService.create(request));
    }

    @Operation(summary = "更新角色",
            description = "只更新请求中非 null 的字段；角色编码不可修改，内置角色不能禁用。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功，返回最新角色"),
            @ApiResponse(responseCode = "400", description = "角色名称为空"),
            @ApiResponse(responseCode = "403", description = "试图禁用内置角色"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @SaCheckPermission(PermissionCodes.ROLE_WRITE)
    @PutMapping("/{id}")
    public Result<SysRole> update(
            @Parameter(description = "角色 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return Result.ok(roleService.update(id, request));
    }

    @Operation(summary = "删除角色", description = "逻辑删除；内置角色和仍有用户使用的角色不能删除。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "内置角色不能删除"),
            @ApiResponse(responseCode = "409", description = "角色仍有用户使用"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @SaCheckPermission(PermissionCodes.ROLE_WRITE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "角色 ID", example = "1") @PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "设置角色权限",
            description = "提交的权限编码列表会整体替换角色原有权限；空数组表示撤销全部权限。ADMIN 恒有全部权限，不能设置。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功，返回最新角色详情"),
            @ApiResponse(responseCode = "400", description = "权限编码不在权限目录中"),
            @ApiResponse(responseCode = "403", description = "ADMIN 角色的权限不能修改"),
            @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @SaCheckPermission(PermissionCodes.ROLE_WRITE)
    @PutMapping("/{id}/permissions")
    public Result<RoleDetailVO> assignPermissions(
            @Parameter(description = "角色 ID", example = "3") @PathVariable Long id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return Result.ok(roleService.assignPermissions(id, request));
    }
}
