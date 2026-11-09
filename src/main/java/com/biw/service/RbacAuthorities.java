package com.biw.service;

import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.RoleCodes;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves what a user may do right now.
 * <p>
 * Read on every check, with no cache: the queries filter on the user and role status, so a
 * disabled account or role stops granting anything immediately, matching the behaviour the
 * user module already promised. {@code ADMIN} is a superuser and answers with the
 * {@code '*'} wildcard, so adding a permission needs no data migration.
 */
@Service
public class RbacAuthorities {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public RbacAuthorities(SysUserRoleMapper userRoleMapper,
                           SysRolePermissionMapper rolePermissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /** Role codes of an enabled user, restricted to enabled roles. */
    public List<String> roleCodes(Long userId) {
        return userRoleMapper.selectActiveRoleCodes(userId);
    }

    /** Whether the user holds the built-in {@code ADMIN} role right now. */
    public boolean isAdmin(Long userId) {
        return roleCodes(userId).contains(RoleCodes.ADMIN);
    }

    /** Effective permission codes; {@code ["*"]} for an admin, empty for an inactive account. */
    public List<String> permissionCodes(Long userId) {
        List<String> roles = roleCodes(userId);
        if (roles.contains(RoleCodes.ADMIN)) {
            return List.of(PermissionCodes.ALL_WILDCARD);
        }
        if (roles.isEmpty()) {
            return List.of();
        }
        return rolePermissionMapper.selectActivePermissionCodes(userId);
    }
}
