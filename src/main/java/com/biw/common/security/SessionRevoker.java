package com.biw.common.security;

/**
 * Ends sessions. Wrapped so the services do not call {@code StpUtil} directly and stay testable.
 */
public interface SessionRevoker {

    /** Invalidates every session of the account (it was disabled, deleted or had its password reset). */
    void revokeAll(Long userId);

    /** Invalidates the caller's own session. */
    void revokeCurrent();
}
