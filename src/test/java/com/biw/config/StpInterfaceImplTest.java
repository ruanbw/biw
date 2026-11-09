package com.biw.config;

import com.biw.service.RbacAuthorities;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sa-Token hands the login id over as whatever was stored in the token session; the interface
 * must accept both a Long and its string form, and delegate the two lists unchanged.
 */
class StpInterfaceImplTest {

    private final RbacAuthorities authorities = mock(RbacAuthorities.class);
    private final StpInterfaceImpl stpInterface = new StpInterfaceImpl(authorities);

    @Test
    void convertsTheLoginIdAndDelegatesBothLists() {
        when(authorities.roleCodes(7L)).thenReturn(List.of("AUDITOR"));
        when(authorities.permissionCodes(7L)).thenReturn(List.of("user:read"));

        assertThat(stpInterface.getRoleList("7", "login")).containsExactly("AUDITOR");
        assertThat(stpInterface.getPermissionList(7L, "login")).containsExactly("user:read");
    }
}
