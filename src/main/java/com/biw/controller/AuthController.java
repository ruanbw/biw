package com.biw.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.api.Result;
import com.biw.config.OpenApiConfig;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;
import com.biw.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录、登出与当前登录用户")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "登录",
            description = "校验用户名和密码，成功后返回 token；后续请求在请求头 `satoken` 中携带 `tokenValue`。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误，或账号已被禁用")
    })
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @Operation(summary = "登出", description = "清除当前会话；未登录时同样返回成功。")
    @ApiResponse(responseCode = "200", description = "已登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return Result.ok();
    }

    @Operation(summary = "当前登录用户", description = "返回当前 token 对应用户的信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "当前用户信息"),
            @ApiResponse(responseCode = "401", description = "未登录或会话已失效")
    })
    @SecurityRequirement(name = OpenApiConfig.TOKEN_SCHEME)
    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.ok(userService.me());
    }
}
