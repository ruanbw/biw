package com.biw.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.biw.common.exception.BizException;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;
import com.biw.mapper.SysUserMapper;
import com.biw.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginVO login(LoginRequest request) {
        SysUser user = lambdaQuery()
                .eq(SysUser::getUsername, normalize(request.username()))
                .one();
        if (user == null
                || !Integer.valueOf(1).equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
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
    public Map<String, Object> listPage(long page, long size, String keyword) {
        Page<SysUser> result = page(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), q -> q
                                .like(SysUser::getUsername, keyword)
                                .or()
                                .like(SysUser::getNickname, keyword))
                        .orderByDesc(SysUser::getCreatedAt));
        return Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "pages", result.getPages());
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

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null ? "" : request.nickname().trim());
        user.setRole(request.role() == null ? "USER" : request.role());
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setDeleted(0);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
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
        boolean roleChanged = request.role() != null && !request.role().equals(user.getRole());
        boolean statusChanged = request.status() != null && !request.status().equals(user.getStatus());
        boolean disabling = statusChanged && request.status() == 0;

        if (id.equals(me) && (roleChanged || disabling)) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "You cannot change your own role or disable yourself");
        }
        if ((roleChanged || disabling)
                && "ADMIN".equals(user.getRole())
                && Integer.valueOf(1).equals(user.getStatus())
                && countActiveAdmins() <= 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }

        if (request.nickname() != null) {
            user.setNickname(request.nickname().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        updateById(user);

        if (disabling) {
            StpUtil.logout(id);
        }
        return getById(id);
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
        Long me = StpUtil.getLoginIdAsLong();
        if (users.stream().anyMatch(u -> u.getId().equals(me))) {
            throw new BizException(HttpStatus.FORBIDDEN, "You cannot delete yourself");
        }

        long deletingAdmins = users.stream()
                .filter(u -> "ADMIN".equals(u.getRole()) && Integer.valueOf(1).equals(u.getStatus()))
                .count();
        if (deletingAdmins > 0 && countActiveAdmins() - deletingAdmins < 1) {
            throw new BizException(HttpStatus.FORBIDDEN,
                    "At least one active administrator must remain");
        }

        // shortcut: admin count check is not locked; fine for one instance, add a lock for multiple instances
        removeByIds(distinct);
        distinct.forEach(id -> StpUtil.logout(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "User not found");
        }
        checkPasswordBytes(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        updateById(user);
        StpUtil.logout(id);
    }

    private long countActiveAdmins() {
        return count(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRole, "ADMIN")
                .eq(SysUser::getStatus, 1));
    }

    private static String normalize(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    private static void checkPasswordBytes(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 bytes");
        }
    }
}
