package com.biw.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.api.Result;
import com.biw.controller.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal authentication endpoints backed by Sa-Token.
 * <p>
 * The hard-coded account is a placeholder: replace {@link #login} with a real
 * user lookup (MyBatis-Plus mapper + password hash) before going live.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final String demoUsername;
    private final String demoPassword;

    public AuthController(@Value("${biw.auth.demo-username:admin}") String demoUsername,
                          @Value("${biw.auth.demo-password:CHANGE_ME}") String demoPassword) {
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        if (!demoUsername.equals(request.username()) || !demoPassword.equals(request.password())) {
            return Result.fail(401, "用户名或密码错误");
        }
        StpUtil.login(request.username());
        return Result.ok(Map.of(
                "tokenName", StpUtil.getTokenName(),
                "tokenValue", StpUtil.getTokenValue()
        ));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(Map.of(
                "loginId", StpUtil.getLoginId(),
                "loggedIn", StpUtil.isLogin()
        ));
    }
}
