package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.convert.UserConverter;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserRoleMapper;
import com.biw.service.RbacAuthorities;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class UserServiceImplLoginTest {

    private final UserServiceImpl service = new UserServiceImpl(
            mock(PasswordEncoder.class), mock(UserConverter.class), mock(StringRedisTemplate.class),
            mock(SysRoleMapper.class), mock(SysRolePermissionMapper.class),
            mock(SysUserRoleMapper.class), mock(RbacAuthorities.class),
            TestDoubles.anonymous(), TestDoubles.sessionIssuer(), TestDoubles.sessionRevoker(),
            TestDoubles.adminGuardLock(), TestDoubles.clock(), TestDoubles.loginThrottlePolicy());

    @Test
    void rejectsAPasswordLongerThanBcryptReads() {
        // BCrypt only looks at the first 72 bytes, so a 73-byte attempt would be accepted as a
        // prefix match. It is rejected before the account is even looked up.
        String tooLong = "a".repeat(73);

        assertThatThrownBy(() -> service.login(new LoginRequest("bob", tooLong)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("72 bytes");
    }
}
