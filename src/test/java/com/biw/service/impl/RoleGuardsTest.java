package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.entity.rbac.RoleCodes;
import com.biw.entity.rbac.RoleStatus;
import com.biw.entity.rbac.SysRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules the role API promises, one case per rule. They are pure decisions, so they are tested
 * here instead of through a database.
 */
class RoleGuardsTest {

    @Test
    void builtInRoleCannotBeDeleted() {
        assertThatThrownBy(() -> RoleGuards.checkDelete(role(RoleCodes.ADMIN, true), 0))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Built-in roles cannot be deleted")
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void customRoleStillAssignedCannotBeDeleted() {
        assertThatThrownBy(() -> RoleGuards.checkDelete(role("AUDITOR", false), 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("assigned to 2 user(s)")
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThatCode(() -> RoleGuards.checkDelete(role("AUDITOR", false), 0))
                .doesNotThrowAnyException();
    }

    @Test
    void builtInRoleCannotBeDisabled() {
        assertThatThrownBy(() -> RoleGuards.checkUpdate(role(RoleCodes.USER, true), RoleStatus.DISABLED, true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Built-in roles cannot be disabled")
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void builtInRoleMayBeRenamedByAnAdministrator() {
        assertThatCode(() -> RoleGuards.checkUpdate(role(RoleCodes.USER, true), null, true))
                .doesNotThrowAnyException();
        assertThatCode(() -> RoleGuards.checkUpdate(role(RoleCodes.USER, true), RoleStatus.ENABLED, true))
                .doesNotThrowAnyException();
    }

    /**
     * ADMIN's permissions are the code-owned wildcard, so {@code sys_role_permission} has no rows
     * for it and the "permissions you hold" ceiling cannot see them. Without this rule a
     * delegated role manager could relabel the superuser role.
     */
    @Test
    void builtInRoleCannotBeTouchedByADelegatedRoleManager() {
        assertThatThrownBy(() -> RoleGuards.checkUpdate(role(RoleCodes.ADMIN, true), null, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Only an administrator can change a built-in role")
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatCode(() -> RoleGuards.checkUpdate(role("AUDITOR", false), null, false))
                .doesNotThrowAnyException();
    }

    @Test
    void builtInRolePermissionsCannotBeEdited() {
        assertThatThrownBy(() -> RoleGuards.checkPermissionsEditable(role(RoleCodes.ADMIN, true)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Built-in roles")
                .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> RoleGuards.checkPermissionsEditable(role(RoleCodes.USER, true)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Built-in roles");
    }

    @Test
    void customRolePermissionsCanBeEdited() {
        assertThatCode(() -> RoleGuards.checkPermissionsEditable(role("AUDITOR", false)))
                .doesNotThrowAnyException();
    }

    private static SysRole role(String code, boolean builtIn) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setBuiltIn(builtIn);
        role.setStatus(RoleStatus.ENABLED);
        return role;
    }
}
