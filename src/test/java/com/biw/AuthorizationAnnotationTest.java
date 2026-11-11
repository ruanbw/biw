package com.biw;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.biw.entity.rbac.PermissionCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * The whole change set moves authorization from a class-level role check to per-method
 * permission codes. A copy-pasted or dropped annotation would still pass the 401 tests, so this
 * pins the code on every `/api/**` handler and makes sure a new endpoint cannot be added
 * without deciding its permission.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthorizationAnnotationTest {

    private static final Map<String, String> EXPECTED = Map.ofEntries(
            entry("GET /api/users", PermissionCodes.USER_READ),
            entry("GET /api/users/options", PermissionCodes.USER_READ),
            entry("POST /api/users", PermissionCodes.USER_WRITE),
            entry("GET /api/users/{id}", PermissionCodes.USER_READ),
            entry("PUT /api/users/{id}", PermissionCodes.USER_WRITE),
            entry("DELETE /api/users/{id}", PermissionCodes.USER_WRITE),
            entry("POST /api/users/batch-delete", PermissionCodes.USER_WRITE),
            entry("POST /api/users/{id}/reset-password", PermissionCodes.USER_WRITE),
            entry("GET /api/roles", PermissionCodes.ROLE_READ),
            entry("GET /api/roles/options", PermissionCodes.ROLE_READ),
            entry("POST /api/roles", PermissionCodes.ROLE_WRITE),
            entry("GET /api/roles/{id}", PermissionCodes.ROLE_READ),
            entry("PUT /api/roles/{id}", PermissionCodes.ROLE_WRITE),
            entry("DELETE /api/roles/{id}", PermissionCodes.ROLE_WRITE),
            entry("PUT /api/roles/{id}/permissions", PermissionCodes.ROLE_WRITE),
            entry("GET /api/permissions", PermissionCodes.PERMISSION_READ));

    /** Self-inspection and session endpoints: login is required, a permission code is not. */
    private static final Set<String> AUTH_ONLY = Set.of(
            "POST /api/auth/login",
            "POST /api/auth/logout",
            "GET /api/auth/me",
            "GET /api/auth/permissions");

    // Actuator registers a second HandlerMapping (controllerEndpointHandlerMapping), so the
    // bean has to be named. This one only carries the request mappings the API is checked on.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyApiHandlerDeclaresItsPermissionCode() {
        Set<String> seen = new HashSet<>();

        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            for (String pattern : mapping.getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    String key = method.name() + " " + pattern;
                    SaCheckPermission permission = handler.getMethodAnnotation(SaCheckPermission.class);
                    if (EXPECTED.containsKey(key)) {
                        assertThat(permission).as(key).isNotNull();
                        assertThat(permission.value()).as(key).contains(EXPECTED.get(key));
                        seen.add(key);
                    } else {
                        assertThat(AUTH_ONLY).as("unexpected API handler: " + key).contains(key);
                        assertThat(permission).as(key).isNull();
                    }
                }
            }
        });

        assertThat(seen).containsExactlyInAnyOrderElementsOf(EXPECTED.keySet());
    }
}
