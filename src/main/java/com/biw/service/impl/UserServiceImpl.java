package com.biw.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.biw.common.api.PageResult;
import com.biw.common.exception.BizException;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    /** Failed logins allowed per account before it is locked for {@link #LOGIN_LOCK}. */
    private static final int MAX_LOGIN_FAILURES = 10;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(5);
    private static final String LOGIN_FAIL_KEY_PREFIX = "biw:login:fail:";
    /**
     * Increments the failure counter, sets the window once, and heals a key an older build left
     * without a TTL. Two commands would leave that key behind when the process dies between them.
     */
    private static final RedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 or redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final StringRedisTemplate redis;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RbacAuthorities rbacAuthorities;
    /** Compared against when the account is unknown or disabled, so both cases cost one BCrypt call. */
    private final String dummyPasswordHash;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserConverter userConverter,
                           StringRedisTemplate redis, SysRoleMapper roleMapper,
                           SysRolePermissionMapper rolePermissionMapper,
                           SysUserRoleMapper userRoleMapper, RbacAuthorities rbacAuthorities) {
        this.passwordEncoder = passwordEncoder;
        this.userConverter = userConverter;
        this.redis = redis;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rbacAuthorities = rbacAuthorities;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        // An over-long password cannot belong to any account (create and reset cap it too), and
        // BCrypt silently ignores bytes past 72, so reject it instead of comparing a prefix.
        checkPasswordBytes(request.password());
        String username = normalize(request.username());
        assertNotLocked(username);

        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        boolean enabled = user != null && user.getStatus() == UserStatus.ENABLED;
        // Always run one BCrypt comparison, even for an unknown or disabled account: returning
        // earlier would answer in ~1ms instead of the ~200ms a real check costs, which tells an
        // attacker whether the username exists.
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                enabled ? user.getPasswordHash() : dummyPasswordHash);
        if (!enabled || !passwordMatches) {
            recordFailure(username, user != null);
            throw new BizException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        redis.delete(LOGIN_FAIL_KEY_PREFIX + username);
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenName(), StpUtil.getTokenValue(),
                StpUtil.getTokenTimeout(), withRoleCodes(user));
    }

    @Override
    public SysUser me() {
        SysUser user = getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "Session is no longer valid");
        }
        return withRoleCodes(user);
    }

    @Override
    public SysUser get(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }
        return withRoleCodes(user);
    }

    @Override
    public PageResult<SysUser> listPage(long page, long size, String keyword) {
        Page<SysUser> result = page(
                // The upper bound matters: a huge page overflows the offset, and a very large
                // one still asks PostgreSQL to walk that many rows before returning nothing.
                new Page<>(Math.min(Math.max(page, 1), 100_000),
                        Math.min(Math.max(size, 1), 100)),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), q -> q
                                .like(SysUser::getUsername, keyword)
                                .or()
                                .like(SysUser::getNickname, keyword))
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
    @Transactional
    public SysUser create(CreateUserRequest request) {
        String username = normalize(request.username());
        if (username == null || username.isBlank()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "Username must not be blank");
        }
        if (count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0) {
            throw new BizException(HttpStatus.CONFLICT, "Username already exists");
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

        SysUser user = userConverter.toEntity(request, username, passwordEncoder.encode(request.password()));
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(HttpStatus.CONFLICT, "Username already exists");
        }
        roleIds.forEach(roleId -> userRoleMapper.insert(user.getId(), roleId));
        return get(user.getId());
    }

    @Override
    @Transactional
    public SysUser update(Long id, UpdateUserRequest request) {
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

        UserGuards.checkUpdate(user.getId(), StpUtil.getLoginIdAsLong(), wasActiveAdmin,
                staysActiveAdmin, roleChange, disabling, this::countActiveAdminsForUpdate);

        userConverter.applyUpdate(request, user);
        // Write only the fields that were sent: writing the whole row back would undo a concurrent
        // password reset or delete, because those changes happened after this row was read.
        boolean updated = lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(request.nickname() != null, SysUser::getNickname, user.getNickname())
                .set(request.status() != null, SysUser::getStatus, user.getStatus())
                .set(SysUser::getUpdatedAt, user.getUpdatedAt())
                .update();
        if (!updated) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (roleIds != null) {
            replaceRoles(id, roleIds);
        }

        if (disabling) {
            StpUtil.logout(id);
        }
        return get(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        batchDelete(List.of(id));
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty() || distinct.size() > 100) {
            throw new BizException(HttpStatus.BAD_REQUEST, "ids must contain between 1 and 100 items");
        }

        boolean deletingSelf = distinct.contains(StpUtil.getLoginIdAsLong());
        long deletingAdmins = userRoleMapper.selectActiveAdminUserIds(distinct).size();
        boolean deletingAnyAdmin = userRoleMapper.selectRoleCodesByUserIds(distinct).stream()
                .anyMatch(row -> RoleCodes.ADMIN.equals(row.getCode()));
        if (deletingAnyAdmin) {
            // A disabled admin still has the role; deleting it is still an admin operation.
            PrivilegeGuards.checkCanModifyAdmin(callerIsAdmin(), true);
        }
        checkCanModifyTargets(distinct);
        UserGuards.checkDelete(deletingSelf, deletingAdmins, this::countActiveAdminsForUpdate);

        removeByIds(distinct);
        distinct.forEach(userRoleMapper::deleteByUserId);
        distinct.forEach(StpUtil::logout);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser target = getById(id);
        if (target == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
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
                .set(SysUser::getUpdatedAt, OffsetDateTime.now())
                .update();
        if (!updated) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }
        StpUtil.logout(id);
    }

    private SysUser withRoleCodes(SysUser user) {
        user.setRoleCodes(userRoleMapper.selectRoleCodes(user.getId()));
        return user;
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
            throw new BizException(HttpStatus.BAD_REQUEST, "Unknown role: " + missing);
        }
        return roles.stream().map(SysRole::getId).toList();
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        roleIds.forEach(roleId -> userRoleMapper.insert(userId, roleId));
    }

    /**
     * Counts enabled admins and locks their rows until the transaction ends. Without the lock two
     * concurrent requests can each see two admins and then remove one, leaving no admin at all —
     * a state the API can never recover from. {@code ORDER BY id} keeps the lock order identical
     * for every caller, so the two cannot deadlock.
     */
    private long countActiveAdminsForUpdate() {
        return userRoleMapper.selectActiveAdminUserIdsForUpdate().size();
    }

    private boolean callerIsAdmin() {
        return rbacAuthorities.isAdmin(StpUtil.getLoginIdAsLong());
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
        List<String> callerPermissions = rbacAuthorities.permissionCodes(StpUtil.getLoginIdAsLong());
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
        List<String> callerPermissions = rbacAuthorities.permissionCodes(StpUtil.getLoginIdAsLong());
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
            throw new BizException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 bytes");
        }
    }

    private void assertNotLocked(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        String failures = redis.opsForValue().get(key);
        if (failures != null && Long.parseLong(failures) >= MAX_LOGIN_FAILURES) {
            // An older build could leave a key without a TTL when the process died between INCR
            // and EXPIRE; without this the account would stay locked forever.
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                redis.expire(key, LOGIN_LOCK);
            }
            throw new BizException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts, try again in " + LOGIN_LOCK.toMinutes() + " minutes");
        }
    }

    /**
     * Counts one failed attempt. Only known accounts get a counter, so unknown usernames cannot
     * fill Redis with keys. The window starts at the first failure and is not extended by later
     * ones: a rolling window would let anyone who knows a username keep that account locked.
     */
    private void recordFailure(String username, boolean knownAccount) {
        if (!knownAccount) {
            return;
        }
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        redis.execute(RECORD_FAILURE_SCRIPT, List.of(key),
                String.valueOf(LOGIN_LOCK.toSeconds()));
    }
}
