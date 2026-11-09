package com.biw.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.biw.common.api.PageResult;
import com.biw.common.api.Result;
import com.biw.config.OpenApiConfig;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.user.dto.BatchIdsRequest;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.SysUser;
import com.biw.service.UserService;
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

@Tag(name = "用户管理", description = "查询接口需要 user:read，写接口需要 user:write")
@SecurityRequirement(name = OpenApiConfig.TOKEN_SCHEME)
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户",
            description = "按创建时间倒序返回；keyword 同时匹配用户名和昵称。每个用户都带 roleCodes。")
    @ApiResponse(responseCode = "200", description = "分页结果")
    @SaCheckPermission(PermissionCodes.USER_READ)
    @GetMapping
    public Result<PageResult<SysUser>> list(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，1-100", example = "20")
            @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "关键字，模糊匹配用户名或昵称", example = "ali")
            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.listPage(page, size, keyword));
    }

    @Operation(summary = "按 ID 查询用户", description = "返回用户及其 roleCodes。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "用户信息"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SaCheckPermission(PermissionCodes.USER_READ)
    @GetMapping("/{id}")
    public Result<SysUser> get(
            @Parameter(description = "用户 ID", example = "1") @PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @Operation(summary = "创建用户",
            description = "用户名不区分大小写且必须唯一。roleCodes 可一次分配多个角色，省略时默认 [USER]，空数组表示不分配角色。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功，返回新用户"),
            @ApiResponse(responseCode = "400", description = "角色编码不存在"),
            @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    @SaCheckPermission(PermissionCodes.USER_WRITE)
    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok(userService.create(request));
    }

    @Operation(summary = "更新用户",
            description = "只更新请求中非 null 的字段；roleCodes 为 null 不修改角色，空数组清空角色。不能修改自己的角色或禁用自己，且至少保留一个启用的管理员。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功，返回最新用户"),
            @ApiResponse(responseCode = "400", description = "角色编码不存在"),
            @ApiResponse(responseCode = "403", description = "操作会移除最后一个启用的管理员，或试图禁用/修改自己的角色"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SaCheckPermission(PermissionCodes.USER_WRITE)
    @PutMapping("/{id}")
    public Result<SysUser> update(
            @Parameter(description = "用户 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @Operation(summary = "删除用户", description = "逻辑删除并清理角色关联；不能删除自己。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "不能删除自己，或会移除最后一个启用的管理员")
    })
    @SaCheckPermission(PermissionCodes.USER_WRITE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "用户 ID", example = "1") @PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除用户", description = "一次最多 100 个；角色关联一并清理，被删除的用户会立即下线。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "包含自己，或会移除最后一个启用的管理员")
    })
    @SaCheckPermission(PermissionCodes.USER_WRITE)
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody BatchIdsRequest request) {
        userService.batchDelete(request.ids());
        return Result.ok();
    }

    @Operation(summary = "重置密码", description = "设置新密码并让该用户的所有会话立即失效。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SaCheckPermission(PermissionCodes.USER_WRITE)
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return Result.ok();
    }
}
