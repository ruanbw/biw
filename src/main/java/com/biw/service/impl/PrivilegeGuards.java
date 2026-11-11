package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.entity.rbac.RoleCodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The ceiling that stops a delegated manager from handing out more than they hold. Without it
 * {@code user:write} is equivalent to ADMIN — create an admin with a chosen password, or reset
 * an existing admin's password — and {@code role:write} can add a permission to a role the
 * caller already holds. It also stops a role manager from editing a role that carries more
 * permissions than the manager holds, which would let it disable the role or strip the codes
 * it cannot submit.
 * <p>
 * Decisions only; the caller supplies its own effective permissions and the target state.
 */
final class PrivilegeGuards {

    private PrivilegeGuards() {
    }

    /** Only a real admin may put the ADMIN role on an account. */
    static void checkCanGrantAdmin(boolean callerIsAdmin, Collection<String> roleCodes) {
        if (!callerIsAdmin && roleCodes != null && roleCodes.contains(RoleCodes.ADMIN)) {
            throw new BizException(BizCode.ADMIN_REQUIRED,
                    "Only an administrator can grant the ADMIN role");
        }
    }

    /**
     * Only a real admin may update, disable, reset or delete an account that holds the ADMIN
     * role. A disabled account still holds it and can be enabled later, so it counts too.
     */
    static void checkCanModifyAdmin(boolean callerIsAdmin, boolean targetHasAdminRole) {
        if (!callerIsAdmin && targetHasAdminRole) {
            throw new BizException(BizCode.ADMIN_REQUIRED,
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
            throw new BizException(BizCode.CANNOT_GRANT_EXCEEDING,
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
            throw new BizException(BizCode.CANNOT_MODIFY_EXCEEDING,
                    "Cannot modify a user with permissions you do not have: " + denied);
        }
    }

    /**
     * A delegated role manager may not edit a role that carries permissions it does not hold.
     * Without this, it can disable the role, or open {@code assignPermissions} and submit only
     * the codes it owns, which silently strips the rest.
     */
    static void checkCanModifyRole(boolean callerIsAdmin, Collection<String> callerPermissions,
                                   Collection<String> rolePermissions) {
        if (callerIsAdmin || rolePermissions == null || rolePermissions.isEmpty()) {
            return;
        }
        String denied = missing(callerPermissions, rolePermissions);
        if (!denied.isEmpty()) {
            throw new BizException(BizCode.CANNOT_MODIFY_EXCEEDING,
                    "Cannot modify a role that grants permissions you do not have: " + denied);
        }
    }

    /**
     * Whether a list row may be acted on by this caller, computed from the same rule the write
     * path enforces. The projection exists so the UI can hide a button instead of offering one
     * that is guaranteed to answer 403 — the check itself stays where it was.
     */
    static boolean canManageRole(boolean callerIsAdmin, boolean builtIn,
                                 Collection<String> callerPermissions, Collection<String> rolePermissions) {
        if (callerIsAdmin) {
            return true;
        }
        return !builtIn && !exceeds(callerPermissions, rolePermissions);
    }

    /** Built-in roles never have their permission set edited, by anyone; the other rules match. */
    static boolean canEditRolePermissions(boolean callerIsAdmin, boolean builtIn,
                                          Collection<String> callerPermissions, Collection<String> rolePermissions) {
        return !builtIn && (callerIsAdmin || !exceeds(callerPermissions, rolePermissions));
    }

    /**
     * Mirror of {@link #checkCanModifyAdmin} plus {@link #checkCanModifyTarget}: an
     * administrator may touch any account, a delegated manager only one that is neither an
     * administrator nor holding a permission it lacks.
     */
    static boolean canManageUser(boolean callerIsAdmin, boolean targetHasAdminRole,
                                 Collection<String> callerPermissions, Collection<String> targetPermissions) {
        if (callerIsAdmin) {
            return true;
        }
        return !targetHasAdminRole && !exceeds(callerPermissions, targetPermissions);
    }

    /** True when the target holds a code the caller does not. */
    static boolean exceeds(Collection<String> callerPermissions, Collection<String> targetPermissions) {
        if (targetPermissions == null || targetPermissions.isEmpty()) {
            return false;
        }
        return !missing(callerPermissions, targetPermissions).isEmpty();
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
