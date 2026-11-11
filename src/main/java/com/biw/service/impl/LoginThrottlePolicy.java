package com.biw.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The ceilings on failed logins. Configuration, not a constant: a shared NAT and an
 * internet-facing single-user deployment need different numbers, and a derived project must be
 * able to change them without editing this class.
 *
 * @param maxAccountFailures failed logins per account before that account is refused
 * @param maxClientFailures  failed logins per client address; higher, because one address may be
 *                           a whole office
 * @param window             how long a bucket lives; a fixed window, so an attacker cannot extend
 *                           it by waiting
 */
@ConfigurationProperties(prefix = "biw.security.login-throttle")
public record LoginThrottlePolicy(int maxAccountFailures, int maxClientFailures, Duration window) {

    public LoginThrottlePolicy {
        if (maxAccountFailures <= 0 || maxClientFailures <= 0) {
            throw new IllegalArgumentException("login-throttle limits must be positive");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("login-throttle window must be positive");
        }
    }
}
