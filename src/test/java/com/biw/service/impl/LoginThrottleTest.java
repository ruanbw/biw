package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The login ceilings, as seen from Java.
 * <p>
 * The decision itself lives in the Lua script (reading both counters and incrementing them has
 * to be one atomic step, otherwise a concurrent burst walks straight through the limit). These
 * tests pin the contract around it: one call, both buckets, both limits, and the refund that
 * keeps a successful login from being counted as a failure. The script body needs a real Redis,
 * which is why the integration layer is the place to test it, not here.
 */
class LoginThrottleTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LoginThrottle throttle = new LoginThrottle(redis, TestDoubles.loginThrottlePolicy());

    @Test
    void refusesWhenTheReservationScriptReportsACeilingReached() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);

        assertThatThrownBy(() -> throttle.attempt("alice", "10.0.0.1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void letsTheAttemptThroughWhenItIsReserved() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        assertThatCode(() -> throttle.attempt("alice", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservesAgainstBothBucketsWithBothLimitsInOneCall() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        throttle.attempt("alice", "10.0.0.1");

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(redis).execute(any(RedisScript.class), keys.capture(), arguments.capture());

        assertThat(keys.getValue()).hasSize(2);
        assertThat(keys.getValue().get(0)).startsWith("biw:login:fail:account:");
        assertThat(keys.getValue().get(1)).isEqualTo("biw:login:fail:client:10.0.0.1");
        // account limit, client limit, window seconds — from TestDoubles.loginThrottlePolicy()
        assertThat(arguments.getValue()).containsExactly("10", "50", "300");
    }

    @Test
    void clearsTheAccountBucketAndRefundsTheClientBucketOnSuccess() {
        throttle.onSuccess("alice", "10.0.0.1");

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(redis).delete(deleted.capture());
        assertThat(deleted.getValue()).startsWith("biw:login:fail:account:");

        // The client bucket is decremented, not deleted: a shared address keeps the failures of
        // everyone else behind it.
        verify(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }
}
