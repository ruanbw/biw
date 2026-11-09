package com.biw.config;

import cn.dev33.satoken.stp.StpInterface;
import com.biw.service.RbacAuthorities;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Roles and permissions come from the RBAC tables on every check, so disabling a user,
 * disabling a role or changing a role's permissions takes effect immediately even if the
 * token is still valid.
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final RbacAuthorities authorities;

    public StpInterfaceImpl(RbacAuthorities authorities) {
        this.authorities = authorities;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return authorities.roleCodes(toUserId(loginId));
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return authorities.permissionCodes(toUserId(loginId));
    }

    private static Long toUserId(Object loginId) {
        return Long.valueOf(loginId.toString());
    }
}
