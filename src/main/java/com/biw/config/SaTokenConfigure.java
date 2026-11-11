package com.biw.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.web.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsUtils;
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
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // A CORS preflight never reaches an endpoint and never carries a token, by
                    // design. Requiring a login here answers it with an error, and the browser
                    // then abandons the real request — an API that works from curl and fails
                    // from the admin bundle. Spring routes the preflight to a dedicated handler,
                    // so nothing behind this check is being exposed.
                    HttpServletRequest request = ClientAddress.currentRequest();
                    if (request != null && CorsUtils.isPreFlightRequest(request)) {
                        return;
                    }
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(WHITE_LIST);
    }
}
