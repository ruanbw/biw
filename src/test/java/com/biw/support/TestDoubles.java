package com.biw.support;

import com.biw.common.audit.AuditRecorder;
import com.biw.common.exception.GlobalExceptionHandler;
import com.biw.common.security.AdminGuardLock;
import com.biw.common.security.CurrentUser;
import com.biw.common.security.SessionIssuer;
import com.biw.common.security.SessionRevoker;
import com.biw.service.impl.LoginThrottlePolicy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The collaborators a service now takes instead of calling statics.
 * <p>
 * Kept in one place so a test states only what it cares about: a service test supplies an actor
 * and a clock, and never needs a request context, a Redis server or a transaction.
 */
public final class TestDoubles {

    /** A fixed instant, so an assertion on a written timestamp is reproducible. */
    public static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    /** The same instant in the type the services write. */
    public static final OffsetDateTime NOW_OFFSET = NOW.atOffset(ZoneOffset.UTC);

    private TestDoubles() {
    }

    public static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static LoginThrottlePolicy loginThrottlePolicy() {
        return new LoginThrottlePolicy(10, 50, Duration.ofMinutes(5));
    }

    public static AuditRecorder auditRecorder() {
        return mock(AuditRecorder.class);
    }

    public static AdminGuardLock adminGuardLock() {
        return mock(AdminGuardLock.class);
    }

    public static SessionIssuer sessionIssuer() {
        return userId -> new SessionIssuer.IssuedToken("satoken", "token", 3600);
    }

    public static SessionRevoker sessionRevoker() {
        return mock(SessionRevoker.class);
    }

    /** An authenticated caller. */
    public static CurrentUser currentUser(Long id, boolean admin, List<String> permissions) {
        CurrentUser user = mock(CurrentUser.class);
        when(user.idOrNull()).thenReturn(id);
        when(user.id()).thenReturn(id);
        when(user.isLogin()).thenReturn(id != null);
        when(user.isAdmin()).thenReturn(admin);
        when(user.permissionCodes()).thenReturn(permissions);
        when(user.roleCodes()).thenReturn(admin ? List.of("ADMIN") : List.of("USER"));
        return user;
    }

    /** A plain user holding exactly the codes given. */
    public static CurrentUser currentUser(Long id, String... permissions) {
        return currentUser(id, false, List.of(permissions));
    }

    /** No caller at all: outside a request. */
    public static CurrentUser anonymous() {
        return currentUser(null, false, List.of());
    }

    /** The handler wired with the collaborators a standalone MockMvc test needs. */
    public static GlobalExceptionHandler handler() {
        return new GlobalExceptionHandler(auditRecorder(), anonymous(), clock());
    }
}
