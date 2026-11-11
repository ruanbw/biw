package com.biw.service.impl;

import com.biw.common.api.OptionVO;
import com.biw.common.exception.BizException;
import com.biw.convert.UserConverter;
import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserStatus;
import com.biw.mapper.SysRoleMapper;
import com.biw.mapper.SysRolePermissionMapper;
import com.biw.mapper.SysUserMapper;
import com.biw.mapper.SysUserRoleMapper;
import com.biw.service.RbacAuthorities;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplOptionsTest {

    private final SysUserMapper mapper = mock(SysUserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(
            mock(PasswordEncoder.class), mock(UserConverter.class), mock(StringRedisTemplate.class),
            mock(SysRoleMapper.class), mock(SysRolePermissionMapper.class),
            mock(SysUserRoleMapper.class), mock(RbacAuthorities.class),
            TestDoubles.anonymous(), TestDoubles.sessionIssuer(), TestDoubles.sessionRevoker(),
            TestDoubles.adminGuardLock(), TestDoubles.clock(), TestDoubles.loginThrottlePolicy());

    @BeforeEach
    void injectMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void searchesAndLabelsByNicknameWithTheUsernameAsSecondaryText() {
        when(mapper.selectOptions("%ali%", 20))
                .thenReturn(List.of(user(7L, "alice", "爱丽丝", UserStatus.ENABLED)));

        List<OptionVO> options = service.listOptions(" ali ", 20, null);

        assertThat(options).containsExactly(new OptionVO(7L, "爱丽丝", "alice", false));
        verify(mapper).selectOptions("%ali%", 20);
    }

    @Test
    void fallsBackToTheUsernameWhenTheNicknameIsBlank() {
        when(mapper.selectOptions(any(), anyInt()))
                .thenReturn(List.of(user(8L, "bob", "  ", UserStatus.ENABLED)));

        assertThat(service.listOptions(null, 20, null))
                .containsExactly(new OptionVO(8L, "bob", null, false));
    }

    @Test
    void escapesLikeMetacharactersInsteadOfLettingThemWidenTheSearch() {
        when(mapper.selectOptions(any(), anyInt())).thenReturn(List.of());

        service.listOptions("50%_off", 20, null);

        verify(mapper).selectOptions("%50\\%\\_off%", 20);
    }

    @Test
    void clampsTheLimitToOnePickerScreen() {
        when(mapper.selectOptions(any(), anyInt())).thenReturn(List.of());

        service.listOptions(null, 500, null);

        verify(mapper).selectOptions(null, 50);
    }

    @Test
    void clampsANonPositiveLimitToOne() {
        when(mapper.selectOptions(any(), anyInt())).thenReturn(List.of());

        service.listOptions(null, 0, null);

        verify(mapper).selectOptions(null, 1);
    }

    @Test
    void hydratesSelectedIdsEvenWhenTheyAreDisabled() {
        when(mapper.selectOptionsByIds(List.of(1L, 2L)))
                .thenReturn(List.of(user(1L, "bob", "", UserStatus.DISABLED)));

        List<OptionVO> options = service.listOptions(null, 20, List.of(1L, 2L));

        assertThat(options).containsExactly(new OptionVO(1L, "bob", null, true));
        verify(mapper, never()).selectOptions(anyString(), anyInt());
    }

    @Test
    void anEmptyIdListQueriesNothing() {
        assertThat(service.listOptions("ignored", 20, List.of())).isEmpty();
        verify(mapper, never()).selectOptionsByIds(any());
    }

    @Test
    void rejectsMoreThanOneHundredIds() {
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> service.listOptions(null, 20, ids))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("100");
    }

    private static SysUser user(Long id, String username, String nickname, UserStatus status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(status);
        return user;
    }
}
