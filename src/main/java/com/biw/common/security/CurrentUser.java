package com.biw.common.security;

import java.util.List;

/**
 * Who is calling, and what they may do right now.
 * <p>
 * Services depend on this instead of {@code StpUtil} directly. That is what makes the write paths
 * unit-testable at all: a test supplies a {@code CurrentUser} and never needs a request context
 * or a running Redis.
 */
public interface CurrentUser {

    /** The authenticated user id. Throws when there is no login, like {@code StpUtil}. */
    Long id();

    /** The authenticated user id, or {@code null} outside a request or when not logged in. */
    Long idOrNull();

    boolean isLogin();

    /** Whether the caller holds the built-in ADMIN role right now. */
    boolean isAdmin();

    /** Effective permission codes; {@code ["*"]} for an admin, empty when nothing is granted. */
    List<String> permissionCodes();

    /** Effective role codes (enabled roles only). */
    List<String> roleCodes();
}
