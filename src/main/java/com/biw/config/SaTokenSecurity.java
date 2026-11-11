package com.biw.config;

import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.security.CurrentUser;
import com.biw.common.security.SessionIssuer;
import com.biw.common.security.SessionRevoker;
import com.biw.service.RbacAuthorities;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The single place that talks to Sa-Token.
 * <p>
 * Everything else depends on {@link CurrentUser}, {@link SessionIssuer} and
 * {@link SessionRevoker}, which keeps the services free of a static dependency on a request
 * context and lets a unit test supply an actor without starting Redis.
 * <p>
 * Roles and permissions are read from the RBAC tables on every check (see
 * {@code RbacAuthorities}), so a disabled account or role takes effect immediately.
 */
@Component
public class SaTokenSecurity implements CurrentUser, SessionIssuer, SessionRevoker {

    private final RbacAuthorities authorities;

    public SaTokenSecurity(RbacAuthorities authorities) {
        this.authorities = authorities;
    }

    @Override
    public Long id() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Long idOrNull() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (RuntimeException outsideARequest) {
            return null;
        }
    }

    @Override
    public boolean isLogin() {
        return idOrNull() != null;
    }

    @Override
    public boolean isAdmin() {
        Long userId = idOrNull();
        return userId != null && authorities.isAdmin(userId);
    }

    @Override
    public List<String> permissionCodes() {
        Long userId = idOrNull();
        return userId == null ? List.of() : authorities.permissionCodes(userId);
    }

    @Override
    public List<String> roleCodes() {
        Long userId = idOrNull();
        return userId == null ? List.of() : authorities.roleCodes(userId);
    }

    @Override
    public IssuedToken login(Long userId) {
        StpUtil.login(userId);
        return new IssuedToken(StpUtil.getTokenName(), StpUtil.getTokenValue(), StpUtil.getTokenTimeout());
    }

    @Override
    public void revokeAll(Long userId) {
        StpUtil.logout(userId);
    }

    @Override
    public void revokeCurrent() {
        StpUtil.logout();
    }
}
