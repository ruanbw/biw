package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserRole;
import com.biw.entity.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules the user API promises. They are pure decisions, so they are tested here instead of
 * through a database.
 */
class UserGuardsTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    private static final LongSupplier NO_ADMIN_COUNT_NEEDED = () -> {
        throw new AssertionError("the admin count must not be queried for this request");
    };

    @Test
    void adminMayRenameThemselvesWithoutTouchingTheAdminCount() {
        assertThatCode(() -> UserGuards.checkUpdate(
                user(ME, UserRole.ADMIN, UserStatus.ENABLED), ME, false, false, NO_ADMIN_COUNT_NEEDED))
                .doesNotThrowAnyException();
    }

    @Test
    void adminMayNotGiveUpTheirOwnRole() {
        assertThatThrownBy(() -> UserGuards.checkUpdate(
                user(ME, UserRole.ADMIN, UserStatus.ENABLED), ME, true, false, NO_ADMIN_COUNT_NEEDED))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role");
    }

    @Test
    void adminMayNotDisableThemselves() {
        assertThatThrownBy(() -> UserGuards.checkUpdate(
                user(ME, UserRole.ADMIN, UserStatus.ENABLED), ME, false, true, NO_ADMIN_COUNT_NEEDED))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role");
    }

    @Test
    void lastAdminCannotBeDemotedOrDisabled() {
        SysUser lastAdmin = user(OTHER, UserRole.ADMIN, UserStatus.ENABLED);

        assertThatThrownBy(() -> UserGuards.checkUpdate(lastAdmin, ME, true, false, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatThrownBy(() -> UserGuards.checkUpdate(lastAdmin, ME, false, true, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
    }

    @Test
    void adminCanStepDownWhileAnotherActiveAdminRemains() {
        assertThatCode(() -> UserGuards.checkUpdate(
                user(OTHER, UserRole.ADMIN, UserStatus.ENABLED), ME, true, false, () -> 2))
                .doesNotThrowAnyException();
    }

    @Test
    void promotingOrDisablingAPlainUserNeverNeedsTheAdminCount() {
        SysUser plainUser = user(OTHER, UserRole.USER, UserStatus.ENABLED);

        assertThatCode(() -> UserGuards.checkUpdate(plainUser, ME, true, false, NO_ADMIN_COUNT_NEEDED))
                .doesNotThrowAnyException();
        assertThatCode(() -> UserGuards.checkUpdate(plainUser, ME, false, true, NO_ADMIN_COUNT_NEEDED))
                .doesNotThrowAnyException();
    }

    @Test
    void adminMayNotDeleteThemselves() {
        assertThatThrownBy(() -> UserGuards.checkDelete(
                List.of(user(ME, UserRole.ADMIN, UserStatus.ENABLED)), ME, NO_ADMIN_COUNT_NEEDED))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot delete yourself");
    }

    @Test
    void lastAdminCannotBeDeleted() {
        assertThatThrownBy(() -> UserGuards.checkDelete(
                List.of(user(OTHER, UserRole.ADMIN, UserStatus.ENABLED)), ME, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
    }

    @Test
    void deletingSeveralAdminsIsRejectedWhenItWouldRemoveAllOfThem() {
        List<SysUser> twoOfThreeAdmins = List.of(
                user(OTHER, UserRole.ADMIN, UserStatus.ENABLED),
                user(3L, UserRole.ADMIN, UserStatus.ENABLED));

        assertThatThrownBy(() -> UserGuards.checkDelete(twoOfThreeAdmins, ME, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator");
        assertThatCode(() -> UserGuards.checkDelete(twoOfThreeAdmins, ME, () -> 3))
                .doesNotThrowAnyException();
    }

    @Test
    void deletingAPlainUserNeverNeedsTheAdminCount() {
        assertThatCode(() -> UserGuards.checkDelete(
                List.of(user(OTHER, UserRole.USER, UserStatus.ENABLED)), ME, NO_ADMIN_COUNT_NEEDED))
                .doesNotThrowAnyException();
    }

    @Test
    void disabledAdminDoesNotCountAsAnAdminBeingRemoved() {
        assertThatCode(() -> UserGuards.checkDelete(
                List.of(user(OTHER, UserRole.ADMIN, UserStatus.DISABLED)), ME, NO_ADMIN_COUNT_NEEDED))
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
