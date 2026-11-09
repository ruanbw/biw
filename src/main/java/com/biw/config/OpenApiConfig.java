package com.biw.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata and the Sa-Token authentication scheme shown in Swagger UI.
 * <p>
 * The scheme is not applied globally: public endpoints (login, logout) and protected ones
 * declare {@code @SecurityRequirement} individually, so the UI shows the padlock correctly.
 */
@Configuration
public class OpenApiConfig {

    /** Request header carrying the Sa-Token value; referenced by {@code @SecurityRequirement}. */
    public static final String TOKEN_SCHEME = "satoken";

    @Bean
    public OpenAPI biwOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("biw 接口文档")
                        .version("v0.0.1")
                        .description("""
                                biw 服务端 HTTP 接口。

                                除登录、登出接口外，所有 `/api/**` 接口都需要登录，并在请求头
                                `satoken` 中携带登录接口返回的 `tokenValue`。

                                权限按“用户 → 角色 → 权限码”三级授予：一个用户可以拥有多个角色，
                                一个角色是一组权限码。接口只声明权限码，不声明角色名，缺少权限码时
                                返回 403。内置 ADMIN 角色拥有 `*`，即全部权限，因此新增权限码无需改数据。

                                所有响应都使用统一结构 `Result`：`code = 0` 表示成功，
                                非 0 时 `data` 为 `null` 且 `message` 说明原因。
                                参数校验失败返回 400，未登录返回 401，无权限返回 403。"""))
                .components(new Components().addSecuritySchemes(TOKEN_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("satoken")
                                .description("登录接口返回的 tokenValue")));
    }
}
