package com.biw.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the Sa-Token interceptor.
 * <p>
 * Everything under {@code /api/**} requires a valid token except the white list below.
 * Session data is stored in Redis through {@code sa-token-redis-template} (auto-configured,
 * no extra bean needed).
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    /** Endpoints callable without being logged in. */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/logout",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter
                        .match("/api/**")
                        .notMatch(WHITE_LIST)
                        .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**")
                .excludePathPatterns(WHITE_LIST);
    }
}
