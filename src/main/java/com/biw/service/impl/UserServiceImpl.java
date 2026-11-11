package com.biw.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.biw.common.api.OptionQuery;
import com.biw.common.api.OptionVO;
import com.biw.common.api.PageBounds;
import com.biw.common.api.PageResult;
import com.biw.common.audit.AuditTrail;
import com.biw.common.audit.Audited;
import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.common.security.AdminGuardLock;
import com.biw.common.security.CurrentUser;
import com.biw.common.security.SessionIssuer;
import com.biw.common.security.SessionRevoker;
import com.biw.common.web.ClientAddress;
import com.biw.convert.UserConverter;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.rbac.RoleCodes;
import com.biw.entity.rbac.SysRole;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserStatus;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserMapper;
import com.biw.mapper.SysUserRoleMapper;
import com.biw.mapper.projection.UserRoleCode;
import com.biw.service.RbacAuthorities;
import com.biw.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final LoginThrottle loginThrottle;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RbacAuthorities rbacAuthorities;
    private final CurrentUser currentUser;
    private final SessionIssuer sessionIssuer;
    private final SessionRevoker sessionRevoker;
    private final AdminGuardLock adminGuardLock;
    private final Clock clock;
    /** Compared against when the account is unknown or disabled, so both cases cost one BCrypt call. */
    private final String dummyPasswordHash;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserConverter userConverter,
                           StringRedisTemplate redis, SysRoleMapper roleMapper,
                           SysRolePermissionMapper rolePermissionMapper,
                           SysUserRoleMapper userRoleMapper, RbacAuthorities rbacAuthorities,
                           CurrentUser currentUser, SessionIssuer sessionIssuer,
                           SessionRevoker sessionRevoker, AdminGuardLock adminGuardLock,
                           Clock clock, LoginThrottlePolicy loginThrottlePolicy) {
        this.passwordEncoder = passwordEncoder;
        this.userConverter = userConverter;
        this.loginThrottle = new LoginThrottle(redis, loginThrottlePolicy);
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rbacAuthorities = rbacAuthorities;
        this.currentUser = currentUser;
        this.sessionIssuer = sessionIssuer;
        this.sessionRevoker = sessionRevoker;
        this.adminGuardLock = adminGuardLock;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        // An over-long password cannot belong to any account (create and reset cap it too), and
        // BCrypt silently ignores bytes past 72, so reject it instead of comparing a prefix.
        checkPasswordBytes(request.password());
        String username = normalize(request.username());
        String clientAddress = ClientAddress.current();
        // A refused attempt is audited by GlobalExceptionHandler from the code carried here.
        loginThrottle.check(username, clientAddress);

        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        boolean enabled = user != null && user.getStatus() == UserStatus.ENABLED;
        // Always run one BCrypt comparison, even for an unknown or disabled account: returning
        // earlier would answer in ~1ms instead of the ~200ms a real check costs, which tells an
        // attacker whether the username exists.
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                enabled ? user.getPasswordHash() : dummyPasswordHash);
        if (!enabled || !passwordMatches) {
            loginThrottle.record(username, clientAddress);
            // The audit event names the outcome; the reason (unknown / disabled / wrong password)
            // is deliberately not part of the response, so it must not be inferred from timing.
            throw new BizException(BizCode.BAD_CREDENTIALS);
        }

        // Only the account bucket: a successful login proves ownership of that name, but a caller
        // must not be able to reset its own client bucket by logging into an account it owns.
        loginThrottle.clear(username);
        SessionIssuer.IssuedToken token = sessionIssuer.login(user.getId());
        return new LoginVO(token.tokenName(), token.tokenValue(), token.timeoutSeconds(), withRoleCodes(user));
    }

    @Override
    public SysUser me() {
        Long id = currentUser.id();
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException(BizCode.SESSION_INVALID);
        }
        return withRoleCodes(user);
    }

    @Override
    public SysUser get(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        return withRoleCodes(user);
    }

    @Override
    public PageResult<SysUser> listPage(long page, long size, String keyword) {
        PageBounds bounds = PageBounds.of(page, size);
        Page<SysUser> result = page(
                new Page<>(bounds.page(), bounds.size()),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), q -> q
                                .apply("username ILIKE {0} OR nickname ILIKE {0}", OptionQuery.pattern(keyword)))
                        .orderByDesc(SysUser::getCreatedAt)
                        // Tiebreaker: rows created in the same millisecond have no stable order in
                        // PostgreSQL, so LIMIT/OFFSET paging could show one twice and skip another.
                        .orderByDesc(SysUser::getId));
        List<SysUser> records = result.getRecords();
        if (!records.isEmpty()) {
            Map<Long, List<String>> rolesByUser = userRoleMapper
                    .selectRoleCodesByUserIds(records.stream().map(SysUser::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(UserRoleCode::getUserId,
                            Collectors.mapping(UserRoleCode::getCode, Collectors.toList())));
            records.forEach(user -> user.setRoleCodes(
                    rolesByUser.getOrDefault(user.getId(), List.of())));
        }
        return new PageResult<>(
                records,
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages());
    }

    @Override
    public List<OptionVO> listOptions(String keyword, int limit, List<Long> ids) {
        if (ids != null) {
            List<Long> distinct = OptionQuery.distinctIds(ids);
            if (distinct.isEmpty()) {
                return List.of();
            }
            return baseMapper.selectOptionsByIds(distinct).stream()
                    .map(UserServiceImpl::toOption)
                    .toList();
        }
        return baseMapper.selectOptions(OptionQuery.pattern(keyword), OptionQuery.limit(limit)).stream()
                .map(UserServiceImpl::toOption)
                .toList();
    }

    @Override
    @Transactional
    @Audited(action = "user.create", targetType = "user", targetIdFromResult = true)
    public SysUser create(CreateUserRequest request) {
        String username = normalize(request.username());
        if (username == null || username.isBlank()) {
            throw new BizException(BizCode.USERNAME_REQUIRED);
        }
        if (count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0) {
            throw new BizException(BizCode.USERNAME_TAKEN);
        }
        checkPasswordBytes(request.password());

        List<String> effectiveCodes = request.roleCodes() == null
                ? List.of(RoleCodes.USER)
                : request.roleCodes();
        if (effectiveCodes.contains(RoleCodes.ADMIN)) {
            PrivilegeGuards.checkCanGrantAdmin(callerIsAdmin(), effectiveCodes);
        }
        List<Long> roleIds = resolveRoleIds(effectiveCodes);
        checkCanAssignRoles(roleIds);

        SysUser user = userConverter.toEntity(request, username,
                passwordEncoder.encode(request.password()), OffsetDateTime.now(clock));
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(BizCode.USERNAME_TAKEN);
        }
        roleIds.forEach(roleId -> userRoleMapper.insert(user.getId(), roleId));
        return get(user.getId());
    }

    @Override
    @Transactional
    @Audited(action = "user.update", targetType = "user", recordChanges = true)
    public SysUser update(Long id, UpdateUserRequest request) {
        // First statement: the admin-count invariant is a property of a set of rows, and this
        // transaction may change that set. See AdminGuardLock.
        adminGuardLock.acquire();

        SysUser user = get(id);

        List<String> assignedRoles = user.getRoleCodes();
        List<String> activeRoles = userRoleMapper.selectActiveRoleCodes(id);
        List<String> newCodes = request.roleCodes() != null ? request.roleCodes() : assignedRoles;
        boolean roleChange = request.roleCodes() != null
                && !new HashSet<>(newCodes).equals(new HashSet<>(assignedRoles));
        UserStatus effectiveStatus = request.status() != null ? request.status() : user.getStatus();
        boolean wasActiveAdmin = user.getStatus() == UserStatus.ENABLED
                && activeRoles.contains(RoleCodes.ADMIN);
        boolean staysActiveAdmin = effectiveStatus == UserStatus.ENABLED
                && newCodes.contains(RoleCodes.ADMIN);
        boolean disabling = effectiveStatus == UserStatus.DISABLED
                && user.getStatus() != UserStatus.DISABLED;

        if (assignedRoles.contains(RoleCodes.ADMIN)
                || (request.roleCodes() != null && request.roleCodes().contains(RoleCodes.ADMIN))) {
            boolean callerIsAdmin = callerIsAdmin();
            PrivilegeGuards.checkCanModifyAdmin(callerIsAdmin, assignedRoles.contains(RoleCodes.ADMIN));
            PrivilegeGuards.checkCanGrantAdmin(callerIsAdmin, request.roleCodes());
        }
        checkCanModifyTargets(List.of(id));
        // Resolve first: an unknown role code should answer 400 even when the same request
        // would also remove the last admin, which the guard would otherwise report as 403.
        List<Long> roleIds = request.roleCodes() == null ? null : resolveRoleIds(request.roleCodes());
        checkCanAssignRoles(roleIds);

        UserGuards.checkUpdate(user.getId(), currentUser.id(), wasActiveAdmin,
                staysActiveAdmin, roleChange, disabling, this::countActiveAdmins);

        AuditTrail.change("nickname", user.getNickname(), request.nickname());
        AuditTrail.change("status", user.getStatus(), request.status());
        AuditTrail.change("roles", assignedRoles, request.roleCodes());

        userConverter.applyUpdate(request, user, OffsetDateTime.now(clock));
        // Write only the fields that were sent: writing the whole row back would undo a concurrent
        // password reset or delete, because those changes happened after this row was read.
        boolean updated = lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(request.nickname() != null, SysUser::getNickname, user.getNickname())
                .set(request.status() != null, SysUser::getStatus, user.getStatus())
                .set(SysUser::getUpdatedAt, user.getUpdatedAt())
                .update();
        if (!updated) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        if (roleIds != null) {
            replaceRoles(id, roleIds);
        }

        if (disabling) {
            sessionRevoker.revokeAll(id);
        }
        return get(id);
    }

    @Override
    @Transactional
    @Audited(action = "user.delete", targetType = "user")
    public void delete(Long id) {
        batchDeleteInternal(List.of(id));
    }

    @Override
    @Transactional
    @Audited(action = "user.batch-delete", targetType = "user")
    public void batchDelete(List<Long> ids) {
        batchDeleteInternal(ids);
    }

    private void batchDeleteInternal(List<Long> ids) {
        adminGuardLock.acquire();

        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty() || distinct.size() > 100) {
            throw new BizException(BizCode.IDS_OUT_OF_RANGE);
        }

        Long me = currentUser.id();
        boolean deletingSelf = distinct.contains(me);
        long deletingAdmins = userRoleMapper.selectActiveAdminUserIds(distinct).size();
        boolean deletingAnyAdmin = userRoleMapper.selectRoleCodesByUserIds(distinct).stream()
                .anyMatch(row -> RoleCodes.ADMIN.equals(row.getCode()));
        if (deletingAnyAdmin) {
            // A disabled admin still has the role; deleting it is still an admin operation.
            PrivilegeGuards.checkCanModifyAdmin(callerIsAdmin(), true);
        }
        checkCanModifyTargets(distinct);
        UserGuards.checkDelete(deletingSelf, deletingAdmins, this::countActiveAdmins);

        removeByIds(distinct);
        distinct.forEach(userRoleMapper::deleteByUserId);
        distinct.forEach(sessionRevoker::revokeAll);
    }

    @Override
    @Transactional
    @Audited(action = "user.reset-password", targetType = "user")
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser target = getById(id);
        if (target == null) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        List<String> targetRoles = userRoleMapper.selectRoleCodes(id);
        if (targetRoles.contains(RoleCodes.ADMIN)) {
            // Even a disabled account keeps the role; enabling it later would restore admin power.
            PrivilegeGuards.checkCanModifyAdmin(callerIsAdmin(), true);
        }
        checkCanModifyTargets(List.of(id));

        checkPasswordBytes(request.newPassword());
        boolean updated = lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getPasswordHash, passwordEncoder.encode(request.newPassword()))
                .set(SysUser::getUpdatedAt, OffsetDateTime.now(clock))
                .update();
        if (!updated) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        sessionRevoker.revokeAll(id);
    }

    private SysUser withRoleCodes(SysUser user) {
        user.setRoleCodes(userRoleMapper.selectRoleCodes(user.getId()));
        return user;
    }

    /** A nickname is optional; when it is blank the username is the only usable label. */
    private static OptionVO toOption(SysUser user) {
        String nickname = user.getNickname() == null ? "" : user.getNickname().trim();
        return new OptionVO(
                user.getId(),
                nickname.isEmpty() ? user.getUsername() : nickname,
                nickname.isEmpty() ? null : user.getUsername(),
                user.getStatus() == UserStatus.DISABLED);
    }

    /**
     * Maps role codes from the request to ids of existing roles. Null means "not supplied"
     * and stays null; an empty list means "no roles" and resolves to no ids. A disabled role
     * is accepted: it grants nothing until enabled, and a user that still holds one can be
     * submitted back unchanged.
     */
    private List<Long> resolveRoleIds(List<String> codes) {
        if (codes == null) {
            return null;
        }
        List<String> distinct = codes.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getCode, distinct)
                // Share-lock the rows until the link insert commits: a concurrent role delete
                // waits here, then re-reads the assignment count and refuses to delete.
                .last("FOR SHARE"));
        if (roles.size() != distinct.size()) {
            Set<String> found = roles.stream().map(SysRole::getCode).collect(Collectors.toSet());
            String missing = distinct.stream()
                    .filter(code -> !found.contains(code))
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BizException(BizCode.UNKNOWN_ROLE, "Unknown role: " + missing);
        }
        return roles.stream().map(SysRole::getId).toList();
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        roleIds.forEach(roleId -> userRoleMapper.insert(userId, roleId));
    }

    /**
     * Counts enabled admins. The caller must hold {@link AdminGuardLock}: without it two
     * concurrent requests can each see two admins and then remove one, leaving no admin at all —
     * a state the API can never recover from.
     */
    private long countActiveAdmins() {
        return userRoleMapper.selectAllActiveAdminUserIds().size();
    }

    private boolean callerIsAdmin() {
        return currentUser.isAdmin();
    }

    /**
     * Stops a delegated user manager from taking over (or deleting) an account that carries
     * permissions the manager lacks. The role sets are read as assigned, not as currently
     * granting: a disabled account or role can be enabled later, so it still counts here.
     */
    private void checkCanModifyTargets(Collection<Long> targetIds) {
        Set<Long> targetRoleIds = new HashSet<>();
        targetIds.forEach(targetId -> targetRoleIds.addAll(userRoleMapper.selectRoleIds(targetId)));
        if (targetRoleIds.isEmpty()) {
            return;
        }
        List<String> targetPermissions =
                rolePermissionMapper.selectPermissionCodesByRoleIds(targetRoleIds);
        if (targetPermissions.isEmpty()) {
            return;
        }
        List<String> callerPermissions = currentUser.permissionCodes();
        PrivilegeGuards.checkCanModifyTarget(
                callerPermissions.contains(PermissionCodes.ALL_WILDCARD),
                callerPermissions,
                targetPermissions);
    }

    /**
     * Prevents a delegated user manager from assigning a role that carries permission codes the
     * manager does not hold. ADMIN is handled separately: its permissions are the wildcard and
     * never appear in sys_role_permission, so a code subset test cannot see them.
     */
    private void checkCanAssignRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<String> callerPermissions = currentUser.permissionCodes();
        if (callerPermissions.contains(PermissionCodes.ALL_WILDCARD)) {
            return;
        }
        List<String> rolePermissions = rolePermissionMapper.selectPermissionCodesByRoleIds(roleIds);
        PrivilegeGuards.checkCanGrantPermissions(false, callerPermissions, rolePermissions);
    }

    private static String normalize(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    private static void checkPasswordBytes(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(BizCode.PASSWORD_TOO_LONG);
        }
    }
}
