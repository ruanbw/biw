package com.biw.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Hardening headers for every response.
 * <p>
 * The service answers with JSON only, so the XSS surface is small, and that is exactly why these
 * matter: a browser that sniffs a JSON body as HTML turns a filename or a nickname into markup.
 * Nothing here depends on the response body, so it is set before the chain runs.
 * <p>
 * {@code Strict-Transport-Security} is sent unconditionally; a browser ignores it when the
 * response arrives over plain HTTP, so it is harmless in local development and effective as soon
 * as the service sits behind TLS.
 * <p>
 * The CSP is {@code default-src 'none'}: this origin serves no HTML and loads no subresource. The
 * Swagger UI is the one exception — it is served from this origin in dev/test and needs scripts,
 * so those paths are skipped instead of breaking the documentation.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Strict-Transport-Security", "max-age=31536000");
        if (!isDocumentation(request)) {
            response.setHeader("Content-Security-Policy", CSP);
        }
        // No user records in a shared cache, in the browser cache, or in a proxy.
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }

    /** Swagger UI and the OpenAPI document: static assets this service hosts itself. */
    private static boolean isDocumentation(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars");
    }
}
