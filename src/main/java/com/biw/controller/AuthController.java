package com.biw.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.api.Result;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;
import com.biw.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.ok(userService.me());
    }
}
