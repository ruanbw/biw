package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.rbac.RoleStatus;
import com.biw.entity.rbac.SysRole;
import org.springframework.http.HttpStatus;

/**
 * The rules that protect the roles the installation depends on: the two built-in roles must stay,
 * a role that still holds assignments cannot be deleted, and {@code ADMIN} is always a superuser.
 * Decisions only — the caller supplies the assignment count it just read.
 */
final class RoleGuards {

    private RoleGuards() {
    }

    /** A built-in role may be renamed, but disabling it would silently take its permissions away. */
    static void checkUpdate(SysRole role, RoleStatus newStatus) {
        if (isBuiltIn(role) && newStatus == RoleStatus.DISABLED) {
            throw new BizException(HttpStatus.FORBIDDEN, "Built-in roles cannot be disabled");
        }
    }

    /**
     * @param assignedUsers users currently holding the role; only checked when the role is deletable
     */
    static void checkDelete(SysRole role, long assignedUsers) {
        if (isBuiltIn(role)) {
            throw new BizException(HttpStatus.FORBIDDEN, "Built-in roles cannot be deleted");
        }
        if (assignedUsers > 0) {
            throw new BizException(HttpStatus.CONFLICT,
                    "Role is assigned to " + assignedUsers + " user(s); remove the assignments first");
        }
    }

    /**
     * Both built-in roles are part of the installation's contract: USER is the default for new
     * accounts, and ADMIN answers every check with the {@code '*'} wildcard. Editing either one at
     * runtime would change what every account gets in a single call.
     */
    static void checkPermissionsEditable(SysRole role) {
        if (isBuiltIn(role)) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "Built-in roles cannot have their permissions edited");
        }
    }

    private static boolean isBuiltIn(SysRole role) {
        return Boolean.TRUE.equals(role.getBuiltIn());
    }
}
