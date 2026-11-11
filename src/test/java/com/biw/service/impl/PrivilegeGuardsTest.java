package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.RoleCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The delegation ceiling: a manager may hand out only what it already holds, and only a real
 * admin may touch the ADMIN role or an active admin account.
 */
class PrivilegeGuardsTest {

    @Test
    void nonAdminCannotGrantAdmin() {
        assertThatThrownBy(() -> PrivilegeGuards.checkCanGrantAdmin(false, List.of(RoleCodes.ADMIN)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Only an administrator can grant the ADMIN role")
                .satisfies(ex -> {
                    assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((BizException) ex).getErrorCode()).isEqualTo(BizCode.ADMIN_REQUIRED.code());
                });
    }

    @Test
    void adminMayGrantAdmin() {
        assertThatCode(() -> PrivilegeGuards.checkCanGrantAdmin(true, List.of(RoleCodes.ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminCannotModifyAnActiveAdmin() {
        assertThatThrownBy(() -> PrivilegeGuards.checkCanModifyAdmin(false, true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Only an administrator can modify an administrator")
                .satisfies(ex -> {
                    assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((BizException) ex).getErrorCode()).isEqualTo(BizCode.ADMIN_REQUIRED.code());
                });
        assertThatCode(() -> PrivilegeGuards.checkCanModifyAdmin(false, false))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminCannotModifyAUserWhoHasMorePermissions() {
        assertThatThrownBy(() -> PrivilegeGuards.checkCanModifyTarget(
                false, List.of(PermissionCodes.USER_READ),
                List.of(PermissionCodes.USER_READ, PermissionCodes.USER_WRITE)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(PermissionCodes.USER_WRITE)
                .satisfies(ex -> {
                    assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((BizException) ex).getErrorCode())
                            .isEqualTo(BizCode.CANNOT_MODIFY_EXCEEDING.code());
                });
    }

    @Test
    void nonAdminMayModifyAUserWhosePermissionsItAlreadyHolds() {
        assertThatCode(() -> PrivilegeGuards.checkCanModifyTarget(
                false, List.of(PermissionCodes.USER_READ), List.of(PermissionCodes.USER_READ)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminCannotGrantAPermissionItDoesNotHold() {
        assertThatThrownBy(() -> PrivilegeGuards.checkCanGrantPermissions(
                false, List.of(PermissionCodes.USER_READ),
                List.of(PermissionCodes.USER_READ, PermissionCodes.USER_WRITE)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(PermissionCodes.USER_WRITE)
                .satisfies(ex -> {
                    assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((BizException) ex).getErrorCode())
                            .isEqualTo(BizCode.CANNOT_GRANT_EXCEEDING.code());
                });
    }

    @Test
    void nonAdminMayGrantItsOwnPermissions() {
        assertThatCode(() -> PrivilegeGuards.checkCanGrantPermissions(
                false, List.of(PermissionCodes.USER_READ), List.of(PermissionCodes.USER_READ)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonAdminCannotEditARoleThatGrantsMorePermissions() {
        assertThatThrownBy(() -> PrivilegeGuards.checkCanModifyRole(
                false, List.of(PermissionCodes.ROLE_WRITE),
                List.of(PermissionCodes.ROLE_WRITE, PermissionCodes.USER_WRITE)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(PermissionCodes.USER_WRITE)
                .satisfies(ex -> {
                    assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((BizException) ex).getErrorCode())
                            .isEqualTo(BizCode.CANNOT_MODIFY_EXCEEDING.code());
                });
    }

    @Test
    void nonAdminMayEditARoleWithinItsOwnPermissions() {
        assertThatCode(() -> PrivilegeGuards.checkCanModifyRole(
                false, List.of(PermissionCodes.ROLE_WRITE), List.of(PermissionCodes.ROLE_WRITE)))
                .doesNotThrowAnyException();
    }

    @Test
    void adminMayEditAnyRole() {
        assertThatCode(() -> PrivilegeGuards.checkCanModifyRole(
                true, List.of(PermissionCodes.ALL_WILDCARD), List.of(PermissionCodes.USER_WRITE)))
                .doesNotThrowAnyException();
    }

    /**
     * The list endpoints show a row's buttons from these flags. If they drift from the rules the
     * write path enforces, the UI either hides an action the API would allow or offers one that
     * is guaranteed to answer 403.
     */
    @Test
    void rowFlagsMirrorTheWritePathRules() {
        List<String> admin = List.of(PermissionCodes.ALL_WILDCARD);

        // A built-in role may be renamed by an administrator and never has its permissions edited.
        assertThat(PrivilegeGuards.canManageRole(true, true, admin, List.of())).isTrue();
        assertThat(PrivilegeGuards.canEditRolePermissions(true, true, admin, List.of())).isFalse();
        assertThat(PrivilegeGuards.canManageRole(false, true, List.of(PermissionCodes.ROLE_WRITE), List.of()))
                .isFalse();

        // A delegated manager may only touch a role it already covers.
        List<String> delegated = List.of(PermissionCodes.ROLE_READ, PermissionCodes.ROLE_WRITE);
        assertThat(PrivilegeGuards.canManageRole(false, false, delegated, List.of(PermissionCodes.ROLE_WRITE)))
                .isTrue();
        assertThat(PrivilegeGuards.canManageRole(false, false, delegated, List.of(PermissionCodes.ROLE_WRITE,
                PermissionCodes.USER_READ))).isFalse();

        // Same rule for accounts, plus: nobody but an administrator touches an administrator.
        List<String> userManager = List.of(PermissionCodes.USER_READ, PermissionCodes.USER_WRITE);
        assertThat(PrivilegeGuards.canManageUser(false, false, userManager, List.of(PermissionCodes.USER_READ)))
                .isTrue();
        assertThat(PrivilegeGuards.canManageUser(false, false, userManager, List.of(PermissionCodes.ROLE_WRITE)))
                .isFalse();
        assertThat(PrivilegeGuards.canManageUser(false, true, userManager, List.of())).isFalse();
        assertThat(PrivilegeGuards.canManageUser(true, true, admin, List.of())).isTrue();
    }

    @Test
    void adminMayGrantAnything() {
        assertThatCode(() -> PrivilegeGuards.checkCanGrantPermissions(
                true, List.of(PermissionCodes.ALL_WILDCARD), List.of(PermissionCodes.ROLE_WRITE)))
                .doesNotThrowAnyException();
    }
}
