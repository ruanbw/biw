package com.biw.service;

import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.RoleCodes;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that decide what a user may do: the built-in admin is the wildcard, an account
 * with no roles gets nothing, and everything else comes from the role-to-permission table.
 */
class RbacAuthoritiesTest {

    private static final Long USER_ID = 7L;

    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
    private final RbacAuthorities authorities =
            new RbacAuthorities(userRoleMapper, rolePermissionMapper);

    @Test
    void adminGetsTheWildcardWithoutReadingRolePermissions() {
        when(userRoleMapper.selectActiveRoleCodes(USER_ID)).thenReturn(List.of(RoleCodes.ADMIN));

        assertThat(authorities.permissionCodes(USER_ID))
                .containsExactly(PermissionCodes.ALL_WILDCARD);
        verify(rolePermissionMapper, never()).selectActivePermissionCodes(USER_ID);
    }

    @Test
    void accountWithoutRolesGetsNothing() {
        when(userRoleMapper.selectActiveRoleCodes(USER_ID)).thenReturn(List.of());

        assertThat(authorities.permissionCodes(USER_ID)).isEmpty();
        verify(rolePermissionMapper, never()).selectActivePermissionCodes(USER_ID);
    }

    @Test
    void customRolesUseThePermissionTable() {
        when(userRoleMapper.selectActiveRoleCodes(USER_ID)).thenReturn(List.of("AUDITOR"));
        when(rolePermissionMapper.selectActivePermissionCodes(USER_ID))
                .thenReturn(List.of(PermissionCodes.USER_READ));

        assertThat(authorities.permissionCodes(USER_ID))
                .containsExactly(PermissionCodes.USER_READ);
    }

    @Test
    void roleCodesDelegateToTheUserRoleMapper() {
        when(userRoleMapper.selectActiveRoleCodes(USER_ID)).thenReturn(List.of(RoleCodes.ADMIN));

        assertThat(authorities.roleCodes(USER_ID)).containsExactly(RoleCodes.ADMIN);
        verify(userRoleMapper).selectActiveRoleCodes(USER_ID);
    }

    @Test
    void adminRoleIsDetectedButAnotherRoleIsNot() {
        when(userRoleMapper.selectActiveRoleCodes(USER_ID))
                .thenReturn(List.of(RoleCodes.ADMIN), List.of("AUDITOR"));

        assertThat(authorities.isAdmin(USER_ID)).isTrue();
        assertThat(authorities.isAdmin(USER_ID)).isFalse();
    }
}
