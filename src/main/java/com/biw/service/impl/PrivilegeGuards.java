package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.rbac.RoleCodes;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The ceiling that stops a delegated manager from handing out more than they hold. Without it
 * {@code user:write} is equivalent to ADMIN — create an admin with a chosen password, or reset
 * an existing admin's password — and {@code role:write} can add a permission to a role the
 * caller already holds.
 * <p>
 * Decisions only; the caller supplies its own effective permissions and the target state.
 */
final class PrivilegeGuards {

    private PrivilegeGuards() {
    }

    /** Only a real admin may put the ADMIN role on an account. */
    static void checkCanGrantAdmin(boolean callerIsAdmin, Collection<String> roleCodes) {
        if (!callerIsAdmin && roleCodes != null && roleCodes.contains(RoleCodes.ADMIN)) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "Only an administrator can grant the ADMIN role");
        }
    }

    /** Only a real admin may update, disable, reset or delete an active admin. */
    static void checkCanModifyAdmin(boolean callerIsAdmin, boolean targetIsActiveAdmin) {
        if (!callerIsAdmin && targetIsActiveAdmin) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "Only an administrator can modify an administrator");
        }
    }

    /** A delegated role manager may only pass on codes it already holds. */
    static void checkCanGrantPermissions(boolean callerIsAdmin, Collection<String> callerPermissions,
                                         Collection<String> requestedPermissions) {
        if (callerIsAdmin || requestedPermissions == null || requestedPermissions.isEmpty()) {
            return;
        }
        String denied = missing(callerPermissions, requestedPermissions);
        if (!denied.isEmpty()) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "Cannot grant permissions you do not have: " + denied);
        }
    }

    /** A delegated manager may not take over an account that holds more permissions than itself. */
    static void checkCanModifyTarget(boolean callerIsAdmin, Collection<String> callerPermissions,
                                     Collection<String> targetPermissions) {
        if (callerIsAdmin || targetPermissions == null || targetPermissions.isEmpty()) {
            return;
        }
        String denied = missing(callerPermissions, targetPermissions);
        if (!denied.isEmpty()) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "Cannot modify a user with permissions you do not have: " + denied);
        }
    }

    private static String missing(Collection<String> allowed, Collection<String> requested) {
        Set<String> held = new HashSet<>(allowed);
        return requested.stream()
                .filter(code -> !held.contains(code))
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
