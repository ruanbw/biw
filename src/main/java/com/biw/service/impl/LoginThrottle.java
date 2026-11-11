package com.biw.service.impl;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;
import com.biw.common.web.ClientAddress;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

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
 * Both numbers come from {@link LoginThrottlePolicy}.
 */
final class LoginThrottle {

    private static final String ACCOUNT_KEY_PREFIX = "biw:login:fail:account:";
    private static final String CLIENT_KEY_PREFIX = "biw:login:fail:client:";

    /**
     * Reserves one attempt against both buckets in a single round trip.
     * <p>
     * Reading the two counters and incrementing them has to be one atomic step. When the check
     * and the write are separate calls, every request of a concurrent burst sees a count below
     * the ceiling and the limit only holds for sequential traffic — which is not what an
     * attacker sends.
     * <p>
     * {@code KEYS[1]} account, {@code KEYS[2]} client; {@code ARGV[1]} account limit,
     * {@code ARGV[2]} client limit, {@code ARGV[3]} window in seconds. Returns 1 when the
     * attempt was reserved, 0 when either bucket is already at its ceiling.
     */
    private static final RedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local accountLimit = tonumber(ARGV[1])
            local clientLimit = tonumber(ARGV[2])
            local window = tonumber(ARGV[3])
            local account = tonumber(redis.call('GET', KEYS[1]) or '0')
            local client = tonumber(redis.call('GET', KEYS[2]) or '0')
            if account >= accountLimit or client >= clientLimit then
              return 0
            end
            for i = 1, #KEYS do
              local count = redis.call('INCR', KEYS[i])
              if count == 1 or redis.call('TTL', KEYS[i]) < 0 then
                redis.call('EXPIRE', KEYS[i], window)
              end
            end
            return 1
            """, Long.class);

    /**
     * Gives one count back to the client bucket after a successful login, because the attempt was
     * reserved but was not a failure. Clamped at zero: a success that was never counted (an older
     * key, a flushed Redis) must not push the counter negative and buy extra attempts.
     */
    private static final RedisScript<Long> REFUND_SCRIPT = new DefaultRedisScript<>("""
            local value = tonumber(redis.call('GET', KEYS[1]) or '0')
            if value > 0 then
              redis.call('DECR', KEYS[1])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final LoginThrottlePolicy policy;

    LoginThrottle(StringRedisTemplate redis, LoginThrottlePolicy policy) {
        this.redis = redis;
        this.policy = policy;
    }

    /**
     * Counts the attempt and refuses it when either bucket is at its ceiling.
     * <p>
     * Called before the password is checked, so a throttled attempt costs no BCrypt call. The
     * attempt is counted even when it succeeds; {@link #onSuccess} gives that count back.
     */
    void attempt(String username, String clientAddress) {
        Long reserved = redis.execute(RESERVE_SCRIPT,
                List.of(accountKey(username), clientKey(clientAddress)),
                String.valueOf(policy.maxAccountFailures()),
                String.valueOf(policy.maxClientFailures()),
                String.valueOf(policy.window().toSeconds()));
        // A null result means the script produced no value (an embedded/test template); treat it
        // as "reserved" rather than turning a Redis quirk into a login outage.
        if (reserved != null && reserved == 0L) {
            throw new BizException(BizCode.LOGIN_THROTTLED,
                    "Too many failed login attempts, try again in " + policy.window().toMinutes() + " minutes");
        }
    }

    /**
     * Clears the account bucket and refunds the client bucket: a successful login proves
     * ownership of the name, and this attempt was not a failure. The rest of the client bucket is
     * deliberately left alone — a caller must not be able to reset it by logging into an account
     * it already controls.
     */
    void onSuccess(String username, String clientAddress) {
        redis.delete(accountKey(username));
        redis.execute(REFUND_SCRIPT, List.of(clientKey(clientAddress)));
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
