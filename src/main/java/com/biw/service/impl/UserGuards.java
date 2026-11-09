package com.biw.service.impl;

import com.biw.common.exception.BizException;
import org.springframework.http.HttpStatus;

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
     * @param wasActiveAdmin   the target was an enabled admin before the update
     * @param staysActiveAdmin the target is still an enabled admin after the update
     * @param activeAdmins     enabled admins at the time of the check; only called when the rule needs it
     */
    static void checkUpdate(Long targetId, Long me, boolean wasActiveAdmin, boolean staysActiveAdmin,
                            boolean roleChange, boolean disabling, LongSupplier activeAdmins) {
        if (me.equals(targetId) && (roleChange || disabling)) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "You cannot change your own role or disable yourself");
        }
        if (wasActiveAdmin && !staysActiveAdmin && activeAdmins.getAsLong() <= 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }
    }

    /**
     * @param activeAdmins enabled admins at the time of the check; only called when the rule needs it
     */
    static void checkDelete(boolean deletingSelf, long deletingAdmins, LongSupplier activeAdmins) {
        if (deletingSelf) {
            throw new BizException(HttpStatus.FORBIDDEN, "You cannot delete yourself");
        }
        if (deletingAdmins > 0 && activeAdmins.getAsLong() - deletingAdmins < 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }
    }
}
