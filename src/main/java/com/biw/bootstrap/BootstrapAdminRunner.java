package com.biw.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.biw.entity.rbac.RoleCodes;
import com.biw.entity.rbac.SysRole;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserStatus;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysUserMapper;
import com.biw.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Creates the first administrator, if one was asked for and the database has no accounts yet.
 * <p>
 * The credential comes from the environment and is never committed. An empty database is the
 * only state this runs in: a second administrator is created through the API, so a restart with
 * the variables still set cannot resurrect a deleted account or reset an existing password.
 * <p>
 * Runs after the container's init script has created the schema and seeded the built-in roles,
 * which is what makes an empty account table possible in the first place.
 * <p>
 * The transaction is opened programmatically rather than with {@code @Transactional} on
 * {@link #run}: a proxy would begin a transaction even when no account is requested, which makes
 * every unit test that boots the context need a database.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    /** The same ceiling the create/reset endpoints enforce; BCrypt reads no further. */
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final String username;
    private final String password;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final TransactionTemplate transactionTemplate;

    public BootstrapAdminRunner(@Value("${biw.bootstrap.admin.username:}") String username,
                                @Value("${biw.bootstrap.admin.password:}") String password,
                                PasswordEncoder passwordEncoder,
                                SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysUserRoleMapper userRoleMapper,
                                TransactionTemplate transactionTemplate) {
        this.username = username;
        this.password = password;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.info("bootstrap administrator skipped: set BIW_BOOTSTRAP_ADMIN_USERNAME and "
                    + "BIW_BOOTSTRAP_ADMIN_PASSWORD to create one on an empty database");
            return;
        }
        validate(username, password);
        transactionTemplate.executeWithoutResult(status -> createIfEmpty());
    }

    private void createIfEmpty() {
        long existing = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery());
        if (existing > 0) {
            log.info("bootstrap administrator skipped: {} account(s) already exist", existing);
            return;
        }

        String normalized = username.trim().toLowerCase(Locale.ROOT);
        SysRole adminRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, RoleCodes.ADMIN));
        if (adminRole == null) {
            throw new IllegalStateException("Built-in role " + RoleCodes.ADMIN
                    + " is missing; the V2 migration did not run");
        }

        OffsetDateTime now = OffsetDateTime.now();
        SysUser admin = new SysUser();
        admin.setUsername(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setNickname("Administrator");
        admin.setStatus(UserStatus.ENABLED);
        admin.setDeleted(0);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userMapper.insert(admin);
        userRoleMapper.insert(admin.getId(), adminRole.getId());

        log.warn("bootstrap administrator '{}' created from environment variables; "
                + "change its password before exposing this service", normalized);
    }

    private static void validate(String rawUsername, String rawPassword) {
        String normalized = rawUsername.trim();
        if (!normalized.matches("^[A-Za-z0-9][A-Za-z0-9_.-]{2,31}$")) {
            throw new IllegalStateException(
                    "BIW_BOOTSTRAP_ADMIN_USERNAME must be 3-32 characters and start with a letter or digit");
        }
        int length = rawPassword.length();
        int bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_PASSWORD_LENGTH || bytes > MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("BIW_BOOTSTRAP_ADMIN_PASSWORD must be at least "
                    + MIN_PASSWORD_LENGTH + " characters and at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }
}
