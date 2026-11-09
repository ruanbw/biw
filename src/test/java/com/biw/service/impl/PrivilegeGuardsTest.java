package com.biw.service.impl;

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
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
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
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
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
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
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
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
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
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
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

    @Test
    void adminMayGrantAnything() {
        assertThatCode(() -> PrivilegeGuards.checkCanGrantPermissions(
                true, List.of(PermissionCodes.ALL_WILDCARD), List.of(PermissionCodes.ROLE_WRITE)))
                .doesNotThrowAnyException();
    }
}
