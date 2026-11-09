package com.biw.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.biw.common.api.PageResult;
import com.biw.common.exception.BizException;
import com.biw.convert.UserConverter;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserRole;
import com.biw.entity.user.UserStatus;
import com.biw.mapper.SysUserMapper;
import com.biw.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    /** Failed logins allowed per account before it is locked for {@link #LOGIN_LOCK}. */
    private static final int MAX_LOGIN_FAILURES = 10;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(5);
    private static final String LOGIN_FAIL_KEY_PREFIX = "biw:login:fail:";

    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final StringRedisTemplate redis;
    /** Compared against when the account is unknown or disabled, so both cases cost one BCrypt call. */
    private final String dummyPasswordHash;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserConverter userConverter,
                           StringRedisTemplate redis) {
        this.passwordEncoder = passwordEncoder;
        this.userConverter = userConverter;
        this.redis = redis;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginVO login(LoginRequest request) {
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
                StpUtil.getTokenTimeout(), user);
    }

    @Override
    public SysUser me() {
        SysUser user = getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "Session is no longer valid");
        }
        return user;
    }

    @Override
    public PageResult<SysUser> listPage(long page, long size, String keyword) {
        Page<SysUser> result = page(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), q -> q
                                .like(SysUser::getUsername, keyword)
                                .or()
                                .like(SysUser::getNickname, keyword))
                        .orderByDesc(SysUser::getCreatedAt)
                        // Tiebreaker: rows created in the same millisecond have no stable order in
                        // PostgreSQL, so LIMIT/OFFSET paging could show one twice and skip another.
                        .orderByDesc(SysUser::getId));
        return new PageResult<>(
                result.getRecords(),
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

        SysUser user = userConverter.toEntity(request, username, passwordEncoder.encode(request.password()));
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(HttpStatus.CONFLICT, "Username already exists");
        }
        return getById(user.getId());
    }

    @Override
    @Transactional
    public SysUser update(Long id, UpdateUserRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }

        Long me = StpUtil.getLoginIdAsLong();
        boolean roleChanged = request.role() != null && request.role() != user.getRole();
        boolean statusChanged = request.status() != null && request.status() != user.getStatus();
        boolean disabling = statusChanged && request.status() == UserStatus.DISABLED;

        UserGuards.checkUpdate(user, me, roleChanged, disabling, this::countActiveAdminsForUpdate);

        userConverter.applyUpdate(request, user);
        // Write only the fields that were sent: writing the whole row back would undo a concurrent
        // password reset or delete, because those changes happened after this row was read.
        boolean updated = lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(request.nickname() != null, SysUser::getNickname, user.getNickname())
                .set(request.role() != null, SysUser::getRole, user.getRole())
                .set(request.status() != null, SysUser::getStatus, user.getStatus())
                .set(SysUser::getUpdatedAt, user.getUpdatedAt())
                .update();
        if (!updated) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (disabling) {
            StpUtil.logout(id);
        }
        return user;
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

        List<SysUser> users = listByIds(distinct);
        UserGuards.checkDelete(users, StpUtil.getLoginIdAsLong(), this::countActiveAdminsForUpdate);

        removeByIds(distinct);
        distinct.forEach(id -> StpUtil.logout(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
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

    /**
     * Counts enabled admins and locks their rows until the transaction ends. Without the lock two
     * concurrent requests can each see two admins and then remove one, leaving no admin at all —
     * a state the API can never recover from. {@code ORDER BY id} keeps the lock order identical
     * for every caller, so the two cannot deadlock.
     */
    private long countActiveAdminsForUpdate() {
        return list(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId)
                .eq(SysUser::getRole, UserRole.ADMIN)
                .eq(SysUser::getStatus, UserStatus.ENABLED)
                .orderByAsc(SysUser::getId)
                .last("FOR UPDATE"))
                .size();
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
        String failures = redis.opsForValue().get(LOGIN_FAIL_KEY_PREFIX + username);
        if (failures != null && Long.parseLong(failures) >= MAX_LOGIN_FAILURES) {
            throw new BizException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts, try again in " + LOGIN_LOCK.toMinutes() + " minutes");
        }
    }

    /**
     * Counts one failed attempt. Only known accounts get a counter, so unknown usernames cannot
     * fill Redis with keys; every failure pushes the expiry out, so the lock ends five minutes
     * after the last try.
     */
    private void recordFailure(String username, boolean knownAccount) {
        if (!knownAccount) {
            return;
        }
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        redis.opsForValue().increment(key);
        redis.expire(key, LOGIN_LOCK);
    }
}
