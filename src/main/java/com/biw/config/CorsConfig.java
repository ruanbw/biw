package com.biw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS, off unless an origin is configured.
 * <p>
 * The development setup does not need it: Vite proxies {@code /api} to this service, so the
 * browser sees one origin. A deployment with a separate front-end origin sets
 * {@code BIW_CORS_ALLOWED_ORIGINS}. An empty value sends no CORS headers at all, which is the
 * safe default — a wildcard here would let any site call the API with the user's token.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${biw.web.cors.allowed-origins:}") String allowedOrigins) {
        // Split on an optional space after each comma: `https://a.com, https://b.com` is the
        // shape people actually write, and an untrimmed origin never matches — silently, because
        // a non-matching origin just gets no CORS header.
        this.allowedOrigins = StringUtils.hasText(allowedOrigins)
                ? allowedOrigins.split("\\s*,\\s*")
                : new String[0];
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("satoken", "Content-Type", RequestIdHeader.NAME)
                .exposedHeaders(RequestIdHeader.NAME)
                .allowCredentials(false)
                .maxAge(3600);
    }

    /** Kept here to avoid importing the web package into a configuration class. */
    private static final class RequestIdHeader {
        private static final String NAME = "X-Request-Id";
    }
}
