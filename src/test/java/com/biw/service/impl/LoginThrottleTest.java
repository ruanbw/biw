package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The login ceilings. The decision is plain Java over two counters now, so it is tested here
 * rather than promised to an integration environment.
 * <p>
 * Policy from {@link TestDoubles#loginThrottlePolicy()}: 10 failures per account, 50 per client,
 * 5 minute window.
 */
class LoginThrottleTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final LoginThrottle throttle = new LoginThrottle(redis, TestDoubles.loginThrottlePolicy());

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void refusesOnceTheAccountReachesItsCeiling() {
        givenCounts("10", "1");

        assertThatThrownBy(() -> throttle.check("alice", "10.0.0.1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Too many failed login attempts")
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizCode.LOGIN_THROTTLED.code()));
    }

    @Test
    void refusesOnceTheClientReachesItsCeiling() {
        givenCounts("0", "50");

        assertThatThrownBy(() -> throttle.check("alice", "10.0.0.1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void allowsAnAttemptBelowBothCeilings() {
        givenCounts("9", "49");

        assertThatCode(() -> throttle.check("alice", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void treatsAnAbsentOrForeignValueAsZero() {
        when(values.multiGet(anyListOfTwo())).thenReturn(Arrays.asList(null, "not-a-number"));

        assertThatCode(() -> throttle.check("alice", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void countsOneFailureAgainstBothBucketsWithAFixedWindow() {
        throttle.onFailure("alice", "10.0.0.1");

        ArgumentCaptor<String> started = ArgumentCaptor.forClass(String.class);
        verify(values, times(2)).setIfAbsent(started.capture(), eq("0"), eq(Duration.ofMinutes(5)));

        ArgumentCaptor<String> incremented = ArgumentCaptor.forClass(String.class);
        verify(values, times(2)).increment(incremented.capture());

        // The counter is created with its window and then bumped, in the same two keys.
        assertThat(started.getAllValues()).isEqualTo(incremented.getAllValues());
        assertThat(started.getAllValues().get(0)).startsWith("biw:login:fail:account:");
        assertThat(started.getAllValues().get(1)).isEqualTo("biw:login:fail:client:10.0.0.1");
    }

    @Test
    void clearsOnlyTheAccountBucketOnSuccess() {
        throttle.onSuccess("alice");

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(redis).delete(deleted.capture());
        assertThat(deleted.getValue()).startsWith("biw:login:fail:account:");
        // The client bucket keeps everyone else's failures behind the same address.
        verify(redis, never()).delete("biw:login:fail:client:10.0.0.1");
    }

    private void givenCounts(String account, String client) {
        when(values.multiGet(anyListOfTwo())).thenReturn(List.of(account, client));
    }

    @SuppressWarnings("unchecked")
    private static List<String> anyListOfTwo() {
        return org.mockito.ArgumentMatchers.argThat(keys -> keys != null && keys.size() == 2);
    }
}
