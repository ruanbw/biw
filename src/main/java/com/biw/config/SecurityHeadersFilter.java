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
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Strict-Transport-Security", "max-age=31536000");
        // No user records in a shared cache, in the browser cache, or in a proxy.
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
