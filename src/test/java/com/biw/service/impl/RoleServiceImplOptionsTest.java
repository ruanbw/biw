package com.biw.service.impl;

import com.biw.common.api.OptionVO;
import com.biw.entity.rbac.RoleStatus;
import com.biw.entity.rbac.SysRole;
import com.biw.mapper.SysPermissionMapper;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserRoleMapper;
import com.biw.service.RbacAuthorities;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplOptionsTest {

    private final SysRoleMapper mapper = mock(SysRoleMapper.class);
    private final RoleServiceImpl service = new RoleServiceImpl(
            mock(SysPermissionMapper.class), mock(SysRolePermissionMapper.class),
            mock(SysUserRoleMapper.class), mock(RbacAuthorities.class),
            TestDoubles.anonymous(), TestDoubles.clock());

    @BeforeEach
    void injectMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void labelsARoleByItsNameAndKeepsTheCodeAsSecondaryText() {
        when(mapper.selectOptions("%AUDIT%", 20))
                .thenReturn(List.of(role(2L, "AUDITOR", "审计员", RoleStatus.ENABLED)));

        assertThat(service.listOptions("AUDIT", 20, null))
                .containsExactly(new OptionVO(2L, "审计员", "AUDITOR", false));
    }

    @Test
    void hydratesSelectedRolesEvenWhenTheyAreDisabled() {
        when(mapper.selectOptionsByIds(List.of(3L)))
                .thenReturn(List.of(role(3L, "LEGACY", "旧角色", RoleStatus.DISABLED)));

        assertThat(service.listOptions(null, 20, List.of(3L)))
                .containsExactly(new OptionVO(3L, "旧角色", "LEGACY", true));
    }

    @Test
    void appliesTheSameLimitAsTheUserPicker() {
        when(mapper.selectOptions(null, 50)).thenReturn(List.of());

        service.listOptions(null, 999, null);

        verify(mapper).selectOptions(null, 50);
    }

    private static SysRole role(Long id, String code, String name, RoleStatus status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setStatus(status);
        return role;
    }
}
