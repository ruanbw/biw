package com.biw.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.biw.common.api.PageResult;
import com.biw.common.exception.BizException;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.RoleCodes;
import com.biw.entity.rbac.RoleStatus;
import com.biw.entity.rbac.SysPermission;
import com.biw.entity.rbac.SysRole;
import com.biw.entity.rbac.dto.AssignPermissionsRequest;
import com.biw.entity.rbac.dto.CreateRoleRequest;
import com.biw.entity.rbac.dto.UpdateRoleRequest;
import com.biw.entity.rbac.vo.RoleDetailVO;
import com.biw.mapper.SysPermissionMapper;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserRoleMapper;
import com.biw.service.RbacAuthorities;
import com.biw.service.RoleService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RbacAuthorities rbacAuthorities;

    public RoleServiceImpl(SysPermissionMapper permissionMapper,
                           SysRolePermissionMapper rolePermissionMapper,
                           SysUserRoleMapper userRoleMapper,
                           RbacAuthorities rbacAuthorities) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rbacAuthorities = rbacAuthorities;
    }

    @Override
    public PageResult<SysRole> listPage(long page, long size, String keyword) {
        Page<SysRole> result = page(
                // The upper bound matters: a huge page overflows the offset, and a very large
                // one still asks PostgreSQL to walk that many rows before returning nothing.
                new Page<>(Math.min(Math.max(page, 1), 100_000),
                        Math.min(Math.max(size, 1), 100)),
                Wrappers.<SysRole>lambdaQuery()
                        .and(StringUtils.hasText(keyword), q -> q
                                .like(SysRole::getCode, keyword)
                                .or()
                                .like(SysRole::getName, keyword))
                        .orderByDesc(SysRole::getCreatedAt)
                        // Tiebreaker: rows created in the same millisecond have no stable order in
                        // PostgreSQL, so LIMIT/OFFSET paging could show one twice and skip another.
                        .orderByDesc(SysRole::getId));
        return new PageResult<>(
                result.getRecords(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages());
    }

    @Override
    public RoleDetailVO get(Long id) {
        SysRole role = require(id);
        List<String> permissionCodes = RoleCodes.ADMIN.equals(role.getCode())
                ? List.of(PermissionCodes.ALL_WILDCARD)
                : rolePermissionMapper.selectPermissionCodes(id);
        return new RoleDetailVO(role, permissionCodes, userRoleMapper.countUsersByRoleId(id));
    }

    @Override
    public SysRole create(CreateRoleRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "Role name must not be blank");
        }
        String description = request.description() == null ? "" : request.description().trim();
        String code = request.code();
        if (count(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code)) > 0) {
            throw new BizException(HttpStatus.CONFLICT, "Role code already exists");
        }

        OffsetDateTime now = OffsetDateTime.now();
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setStatus(RoleStatus.ENABLED);
        role.setBuiltIn(false);
        role.setDeleted(0);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        try {
            save(role);
        } catch (DuplicateKeyException e) {
            // Two requests can pass the count above at the same time; the unique index decides.
            throw new BizException(HttpStatus.CONFLICT, "Role code already exists");
        }
        return require(role.getId());
    }

    @Override
    public SysRole update(Long id, UpdateRoleRequest request) {
        SysRole role = require(id);
        RoleGuards.checkUpdate(role, request.status());
        checkCanModifyRole(role);

        String name = null;
        if (request.name() != null) {
            name = request.name().trim();
            if (name.isEmpty()) {
                throw new BizException(HttpStatus.BAD_REQUEST, "Role name must not be blank");
            }
        }
        String description = request.description() == null ? null : request.description().trim();

        // Write only the fields that were sent: writing the whole row back would undo a concurrent
        // permission change or delete, because those happened after this row was read.
        boolean updated = lambdaUpdate()
                .eq(SysRole::getId, id)
                .set(request.name() != null, SysRole::getName, name)
                .set(request.description() != null, SysRole::getDescription, description)
                .set(request.status() != null, SysRole::getStatus, request.status())
                .set(SysRole::getUpdatedAt, OffsetDateTime.now())
                .update();
        if (!updated) {
            throw new BizException(HttpStatus.NOT_FOUND, "Role not found");
        }
        return require(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Serialize against assignPermissions and against a user update that is resolving this
        // role, so the count cannot be invalidated between the check and the logic delete.
        SysRole role = requireForUpdate(id);
        RoleGuards.checkDelete(role, userRoleMapper.countUsersByRoleId(id));
        checkCanModifyRole(role);

        removeById(id);
        // A logic delete keeps the row, so the FK cascade never fires and the links need removing.
        // This also drops links left by users that were deleted before this role.
        userRoleMapper.deleteByRoleId(id);
        rolePermissionMapper.deleteByRoleId(id);
    }

    @Override
    @Transactional
    public RoleDetailVO assignPermissions(Long id, AssignPermissionsRequest request) {
        // The row lock also serializes two replacements, otherwise both could insert and the
        // resulting set would be their union instead of the set the later caller submitted.
        SysRole role = requireForUpdate(id);
        RoleGuards.checkPermissionsEditable(role);

        List<String> callerPermissions = rbacAuthorities.permissionCodes(StpUtil.getLoginIdAsLong());
        PrivilegeGuards.checkCanModifyRole(
                callerPermissions.contains(PermissionCodes.ALL_WILDCARD), callerPermissions,
                rolePermissionMapper.selectPermissionCodes(id));

        List<String> codes = request.permissionCodes().stream().distinct().toList();
        List<SysPermission> permissions = codes.isEmpty()
                ? List.of()
                : permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .in(SysPermission::getCode, codes));
        if (permissions.size() != codes.size()) {
            Set<String> known = permissions.stream()
                    .map(SysPermission::getCode)
                    .collect(Collectors.toSet());
            String missing = codes.stream()
                    .filter(code -> !known.contains(code))
                    .collect(Collectors.joining(", "));
            throw new BizException(HttpStatus.BAD_REQUEST, "Unknown permission: " + missing);
        }

        PrivilegeGuards.checkCanGrantPermissions(
                callerPermissions.contains(PermissionCodes.ALL_WILDCARD), callerPermissions, codes);

        rolePermissionMapper.deleteByRoleId(id);
        permissions.forEach(permission -> rolePermissionMapper.insert(id, permission.getId()));
        // The permissions changed even though no column of sys_role did, so the timestamp must too.
        lambdaUpdate()
                .eq(SysRole::getId, id)
                .set(SysRole::getUpdatedAt, OffsetDateTime.now())
                .update();
        return get(id);
    }

    /**
     * A delegated role manager may only edit a role whose current permissions it already holds.
     * The current set is checked, not the request: otherwise it could disable a stronger role,
     * or replace its permissions with the subset it owns and silently strip the rest.
     */
    private void checkCanModifyRole(SysRole role) {
        List<String> callerPermissions = rbacAuthorities.permissionCodes(StpUtil.getLoginIdAsLong());
        PrivilegeGuards.checkCanModifyRole(
                callerPermissions.contains(PermissionCodes.ALL_WILDCARD),
                callerPermissions,
                rolePermissionMapper.selectPermissionCodes(role.getId()));
    }

    /**
     * Reads a role or answers 404. The permission query in {@link #get(Long)} is skipped here
     * because the write paths do not need it.
     */
    private SysRole require(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "Role not found");
        }
        return role;
    }

    /** Locks the role row for the rest of the transaction; 404 when it is gone. */
    private SysRole requireForUpdate(Long id) {
        SysRole role = lambdaQuery().eq(SysRole::getId, id).last("FOR UPDATE").one();
        if (role == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "Role not found");
        }
        return role;
    }
}
