package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserRole;
import com.biw.entity.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules the user API promises, one case per rule. They are pure decisions, so they are tested
 * here instead of through a database.
 */
class UserGuardsTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    @Test
    void adminMayRenameThemselves() {
        assertThatCode(() -> UserGuards.checkUpdate(
                user(ME, UserRole.ADMIN, UserStatus.ENABLED), ME, false, false, () -> 1))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCannotRemoveTheirOwnAdminPower() {
        SysUser me = user(ME, UserRole.ADMIN, UserStatus.ENABLED);

        assertThatThrownBy(() -> UserGuards.checkUpdate(me, ME, true, false, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role");
        assertThatThrownBy(() -> UserGuards.checkUpdate(me, ME, false, true, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role");
    }

    @Test
    void lastActiveAdminCannotLoseAdminPowerButAnotherOneCan() {
        SysUser otherAdmin = user(OTHER, UserRole.ADMIN, UserStatus.ENABLED);

        assertThatThrownBy(() -> UserGuards.checkUpdate(otherAdmin, ME, true, false, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatThrownBy(() -> UserGuards.checkUpdate(otherAdmin, ME, false, true, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatCode(() -> UserGuards.checkUpdate(otherAdmin, ME, true, false, () -> 2))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminTargetsAreNotLimitedByTheAdminCount() {
        SysUser plainUser = user(OTHER, UserRole.USER, UserStatus.ENABLED);

        assertThatCode(() -> UserGuards.checkUpdate(plainUser, ME, true, false, () -> 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> UserGuards.checkUpdate(plainUser, ME, false, true, () -> 1))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCannotDeleteThemselves() {
        assertThatThrownBy(() -> UserGuards.checkDelete(
                List.of(user(ME, UserRole.ADMIN, UserStatus.ENABLED)), ME, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot delete yourself");
    }

    @Test
    void deletingAdminsMustLeaveOneActive() {
        List<SysUser> oneAdmin = List.of(user(OTHER, UserRole.ADMIN, UserStatus.ENABLED));
        List<SysUser> twoAdmins = List.of(
                user(OTHER, UserRole.ADMIN, UserStatus.ENABLED),
                user(3L, UserRole.ADMIN, UserStatus.ENABLED));

        assertThatThrownBy(() -> UserGuards.checkDelete(oneAdmin, ME, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatThrownBy(() -> UserGuards.checkDelete(twoAdmins, ME, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatCode(() -> UserGuards.checkDelete(twoAdmins, ME, () -> 3))
                .doesNotThrowAnyException();
    }

    @Test
    void deletingNonAdminsIsNotLimitedByTheAdminCount() {
        assertThatCode(() -> UserGuards.checkDelete(
                List.of(user(OTHER, UserRole.USER, UserStatus.ENABLED)), ME, () -> 1))
                .doesNotThrowAnyException();
        // An admin who is already disabled is not one of the admins being taken away.
        assertThatCode(() -> UserGuards.checkDelete(
                List.of(user(OTHER, UserRole.ADMIN, UserStatus.DISABLED)), ME, () -> 1))
                .doesNotThrowAnyException();
    }

    private static SysUser user(long id, UserRole role, UserStatus status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
