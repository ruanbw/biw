package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.common.web.ClientAddress;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * Counts failed logins per account and per client address.
 * <p>
 * Failures are counted for names that do not exist too. That is deliberate: a counter that only
 * existed for real accounts made the 429 an account oracle, which undid the effort spent on
 * {@code dummyPasswordHash} to make a failed login cost the same either way. Counting both also
 * means a targeted lockout costs the attacker the client bucket, not just ten requests.
 * <p>
 * The account bucket only slows a guess at one account; the client bucket is what actually stops
 * password spraying, so it is set higher — one address may be a shared NAT carrying many people.
 * Both numbers come from {@link LoginThrottlePolicy}.
 * <p>
 * <strong>Why not a Lua script.</strong> Reading a counter and incrementing it atomically would
 * stop a concurrent burst from overshooting the ceiling by the number of requests in flight. This
 * is a single-instance service whose ceiling exists to make guessing slow, not to enforce a quota:
 * an overshoot of a handful of BCrypt calls changes nothing an attacker can use, and the cost of
 * the guarantee was the only untestable code in the project. Counters live in Redis rather than in
 * memory because the session store is already there and a restart should not hand back attempts.
 */
final class LoginThrottle {

    private static final String ACCOUNT_KEY_PREFIX = "biw:login:fail:account:";
    private static final String CLIENT_KEY_PREFIX = "biw:login:fail:client:";

    private final StringRedisTemplate redis;
    private final LoginThrottlePolicy policy;

    LoginThrottle(StringRedisTemplate redis, LoginThrottlePolicy policy) {
        this.redis = redis;
        this.policy = policy;
    }

    /**
     * Refuses the attempt when either bucket already holds its ceiling.
     * <p>
     * Called before the password is checked, so a throttled attempt costs no BCrypt call.
     */
    void check(String username, String clientAddress) {
        List<String> counts = redis.opsForValue()
                .multiGet(List.of(accountKey(username), clientKey(clientAddress)));
        if (counts == null) {
            return;
        }
        long account = parseCount(counts.get(0));
        long client = parseCount(counts.size() > 1 ? counts.get(1) : null);
        if (account >= policy.maxAccountFailures() || client >= policy.maxClientFailures()) {
            throw new BizException(BizCode.LOGIN_THROTTLED,
                    "Too many failed login attempts, try again in " + policy.window().toMinutes() + " minutes");
        }
    }

    /** Counts one failure against both buckets, each with a fixed window from its first failure. */
    void onFailure(String username, String clientAddress) {
        increment(accountKey(username));
        increment(clientKey(clientAddress));
    }

    /**
     * A successful login proves ownership of the name, so the account bucket is cleared. The
     * client bucket is left alone: a caller must not be able to reset a shared address's counter
     * by logging into an account it already controls.
     */
    void onSuccess(String username) {
        redis.delete(accountKey(username));
    }

    /**
     * Starts the window and the counter in one command sequence.
     * <p>
     * {@code setIfAbsent} carries the TTL, so a counter can never be created without one — with
     * {@code INCR} followed by a conditional {@code EXPIRE}, a connection lost between the two
     * would leave a key that never expires, i.e. a permanent lockout.
     */
    private void increment(String key) {
        redis.opsForValue().setIfAbsent(key, "0", policy.window());
        redis.opsForValue().increment(key);
    }

    private static long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException notOurs) {
            // Something else wrote this key; treat it as empty rather than failing the login.
            return 0;
        }
    }

    /**
     * The account name is hashed: a login accepts any string up to 64 characters, so using it
     * verbatim would let a caller put arbitrary bytes (and by consequence any number of keys) into
     * Redis, and would leave usernames in a cache dump.
     */
    private static String accountKey(String username) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(username.getBytes(StandardCharsets.UTF_8));
            return ACCOUNT_KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to be present in the JDK", e);
        }
    }

    /** The address is written verbatim: it is already normalised by the container. */
    private static String clientKey(String clientAddress) {
        return CLIENT_KEY_PREFIX + (clientAddress == null ? ClientAddress.UNKNOWN : clientAddress);
    }
}
