package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserRole;
import com.biw.entity.user.UserStatus;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * The rules that keep an installation usable: an admin cannot lock themselves out, and the last
 * enabled admin cannot be removed. Decisions only — the caller supplies the admin count, which it
 * must have read under a row lock inside its own transaction.
 */
final class UserGuards {

    private UserGuards() {
    }

    /**
     * @param activeAdmins enabled admins at the time of the check; only called when the rule needs it
     */
    static void checkUpdate(SysUser target, Long me, boolean roleChange, boolean disabling,
                            LongSupplier activeAdmins) {
        boolean removingAdminPower = roleChange || disabling;
        if (me.equals(target.getId()) && removingAdminPower) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "You cannot change your own role or disable yourself");
        }
        if (removingAdminPower && isActiveAdmin(target) && activeAdmins.getAsLong() <= 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }
    }

    /**
     * @param activeAdmins enabled admins at the time of the check; only called when the rule needs it
     */
    static void checkDelete(List<SysUser> targets, Long me, LongSupplier activeAdmins) {
        if (targets.stream().anyMatch(user -> me.equals(user.getId()))) {
            throw new BizException(HttpStatus.FORBIDDEN, "You cannot delete yourself");
        }
        long deletingAdmins = targets.stream().filter(UserGuards::isActiveAdmin).count();
        if (deletingAdmins > 0 && activeAdmins.getAsLong() - deletingAdmins < 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }
    }

    private static boolean isActiveAdmin(SysUser user) {
        return user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ENABLED;
    }
}
