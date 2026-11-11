package com.biw.service.impl;

import com.biw.common.exception.BizException;
import org.junit.jupiter.api.Test;
import com.biw.support.TestDoubles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The login ceilings. Both buckets are read on every attempt, and they carry different limits:
 * the account bucket slows a guess at one name, the client bucket is what stops password
 * spraying across many names.
 */
class LoginThrottleTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LoginThrottle throttle = new LoginThrottle(redis, TestDoubles.loginThrottlePolicy());

    {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void refusesOnceTheAccountReachesItsCeiling() {
        when(values.multiGet(anyList())).thenReturn(List.of("10", "0"));

        assertThatThrownBy(() -> throttle.check("alice", "10.0.0.1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void allowsAClientAddressThatIsBusyButBelowItsOwnHigherCeiling() {
        // 10 would already lock an account. The client ceiling is higher because one address can
        // be a shared NAT, so this attempt must go through.
        when(values.multiGet(anyList())).thenReturn(List.of("2", "10"));

        assertThatCode(() -> throttle.check("alice", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void refusesOnceTheClientAddressReachesItsCeiling() {
        when(values.multiGet(anyList())).thenReturn(List.of("0", "50"));

        assertThatThrownBy(() -> throttle.check("alice", "10.0.0.1"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void countsAnAttemptAgainstTheAccountAndTheClientAddress() {
        throttle.record("alice", "10.0.0.1");

        verify(redis).execute(any(), anyList(), any());
    }
}
