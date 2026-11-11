package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
    void plainRenameNeverNeedsTheAdminCount() {
        assertThatCode(() -> UserGuards.checkUpdate(OTHER, ME, false, false, false, false,
                () -> {
                    throw new AssertionError("admin count must not be read");
                }))
                .doesNotThrowAnyException();
    }

    @Test
    void adminMayRenameThemselves() {
        assertThatCode(() -> UserGuards.checkUpdate(ME, ME, true, true, false, false, () -> 1))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCannotRemoveTheirOwnAdminPower() {
        assertThatThrownBy(() -> UserGuards.checkUpdate(ME, ME, true, false, true, false, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.SELF_LOCKOUT.code()));
        assertThatThrownBy(() -> UserGuards.checkUpdate(ME, ME, true, false, false, true, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot change your own role")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.SELF_LOCKOUT.code()));
    }

    @Test
    void lastActiveAdminCannotLoseAdminPowerButAnotherOneCan() {
        assertThatThrownBy(() -> UserGuards.checkUpdate(OTHER, ME, true, false, true, false, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.LAST_ADMIN.code()));
        assertThatThrownBy(() -> UserGuards.checkUpdate(OTHER, ME, true, false, false, true, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.LAST_ADMIN.code()));
        assertThatCode(() -> UserGuards.checkUpdate(OTHER, ME, true, false, true, false, () -> 2))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminTargetsAreNotLimitedByTheAdminCount() {
        assertThatCode(() -> UserGuards.checkUpdate(OTHER, ME, false, false, true, false,
                () -> {
                    throw new AssertionError("admin count must not be read");
                }))
                .doesNotThrowAnyException();
        assertThatCode(() -> UserGuards.checkUpdate(OTHER, ME, false, false, false, true,
                () -> {
                    throw new AssertionError("admin count must not be read");
                }))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCannotDeleteThemselves() {
        assertThatThrownBy(() -> UserGuards.checkDelete(true, 1, () -> {
            throw new AssertionError("admin count must not be read");
        }))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("cannot delete yourself")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.SELF_DELETE.code()));
    }

    @Test
    void deletingAdminsMustLeaveOneActive() {
        assertThatThrownBy(() -> UserGuards.checkDelete(false, 1, () -> 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.LAST_ADMIN.code()));
        assertThatThrownBy(() -> UserGuards.checkDelete(false, 2, () -> 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("At least one active administrator")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.LAST_ADMIN.code()));
        assertThatCode(() -> UserGuards.checkDelete(false, 2, () -> 3))
                .doesNotThrowAnyException();
    }

    @Test
    void deletingNonAdminsNeverNeedsTheAdminCount() {
        assertThatCode(() -> UserGuards.checkDelete(false, 0, () -> {
            throw new AssertionError("admin count must not be read");
        }))
                .doesNotThrowAnyException();
    }
}
