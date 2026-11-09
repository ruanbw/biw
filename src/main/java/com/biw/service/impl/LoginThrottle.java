package com.biw.service.impl;

import com.biw.common.exception.BizException;
import com.biw.common.web.ClientAddress;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * The ceilings on failed logins: one per account and one per client address.
 * <p>
 * Every failed attempt is counted, including attempts against usernames that do not exist. That
 * is deliberate. A counter that only existed for real accounts made the 429 an account oracle:
 * ten requests told an attacker whether a username was registered, which undid the effort spent
 * on {@code dummyPasswordHash} to make a failed login cost the same either way. Counting both
 * also means a targeted lockout costs the attacker the client bucket, not just ten requests.
 * <p>
 * The account bucket only slows a guess at one account; the client bucket is what actually stops
 * password spraying, so it is set higher — one address may be a shared NAT carrying many people.
 */
final class LoginThrottle {

    /** Failed logins per account before that account is refused for {@link #WINDOW}. */
    static final int MAX_ACCOUNT_FAILURES = 10;

    /** Failed logins per client address; higher, because one address may be a whole office. */
    static final int MAX_CLIENT_FAILURES = 50;

    /** How long a bucket lives. Fixed window, so it cannot be extended by the attacker. */
    static final Duration WINDOW = Duration.ofMinutes(5);

    private static final String ACCOUNT_KEY_PREFIX = "biw:login:fail:account:";
    private static final String CLIENT_KEY_PREFIX = "biw:login:fail:client:";

    /**
     * Increments both buckets and sets the window on the first failure. One script for both keys:
     * two commands could leave a key behind when the process dies in between, and a key without a
     * TTL would refuse that account forever.
     */
    private static final RedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
              local count = redis.call('INCR', KEYS[i])
              if count == 1 or redis.call('TTL', KEYS[i]) < 0 then
                redis.call('EXPIRE', KEYS[i], ARGV[1])
              end
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    LoginThrottle(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Refuses the attempt when either bucket is at its ceiling. Read before the password is
     * checked, so a locked account costs no BCrypt call.
     */
    void check(String username, String clientAddress) {
        List<String> counts = redis.opsForValue()
                .multiGet(List.of(accountKey(username), clientKey(clientAddress)));
        if (counts == null) {
            return;
        }
        if (atLeast(counts.get(0), MAX_ACCOUNT_FAILURES) || atLeast(counts.get(1), MAX_CLIENT_FAILURES)) {
            throw new BizException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts, try again in " + WINDOW.toMinutes() + " minutes");
        }
    }

    /** Counts one failed attempt against both buckets. */
    void record(String username, String clientAddress) {
        redis.execute(RECORD_FAILURE_SCRIPT,
                List.of(accountKey(username), clientKey(clientAddress)),
                String.valueOf(WINDOW.toSeconds()));
    }

    /**
     * Forgets the account's failures: a successful login proves ownership of the name. The client
     * bucket is deliberately left alone, otherwise a caller could reset it by logging into an
     * account it already controls.
     */
    void clear(String username) {
        redis.delete(accountKey(username));
    }

    private static int parse(String count) {
        return count == null ? 0 : Integer.parseInt(count);
    }

    private static boolean atLeast(String count, int limit) {
        return count != null && parse(count) >= limit;
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
