package com.biw.common.error;

import org.springframework.http.HttpStatus;

/**
 * Every failure the API can answer with, in one table.
 * <p>
 * One entry carries three things that used to be unrelated strings:
 * <ul>
 *   <li>{@code status} — the HTTP status, kept because callers and proxies understand it;</li>
 *   <li>{@code code} — a stable machine identifier. The frontend maps it to a localized message,
 *       tests assert on it, and it no longer changes when a message is reworded;</li>
 *   <li>{@code auditEvent} — the event name written by the audit trail when this failure is
 *       recorded, or {@code null} when the failure is routine and not worth an audit record.</li>
 * </ul>
 * Keeping them in one enum is what stops the two vocabularies from drifting apart: a derived
 * project adds a code here and both the API and the audit log pick it up.
 * <p>
 * Conventions: {@code code} is SCREAMING_SNAKE_CASE and grouped by area; {@code auditEvent} is
 * {@code domain.action.outcome}.
 */
public enum BizCode {

    // --- 1000s: request shape -------------------------------------------------
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", null),
    MALFORMED_BODY(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Malformed or unreadable request body", null),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
            "Unsupported content type", null),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Missing required parameter", null),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Invalid parameter type", null),
    PAGE_TOO_DEEP(HttpStatus.BAD_REQUEST, "PAGE_TOO_DEEP",
            "Page is too deep; narrow the search instead of paging further", null),
    IDS_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "IDS_OUT_OF_RANGE",
            "ids must contain between 1 and 100 items", null),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "USERNAME_REQUIRED", "Username must not be blank", null),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG",
            "Password must not exceed 72 bytes", null),
    ROLE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "ROLE_NAME_REQUIRED", "Role name must not be blank", null),
    UNKNOWN_ROLE(HttpStatus.BAD_REQUEST, "UNKNOWN_ROLE", "Unknown role", null),
    UNKNOWN_PERMISSION(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "Unknown permission", null),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "Endpoint not found", null),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", null),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "No acceptable response media type", null),

    // --- 2000s: authentication ------------------------------------------------
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid username or password",
            "auth.login.rejected"),
    SESSION_INVALID(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Session is no longer valid",
            "auth.session.rejected"),
    MISSING_CREDENTIAL(HttpStatus.UNAUTHORIZED, "MISSING_CREDENTIAL", "Missing login credential",
            "auth.token.rejected"),
    INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIAL", "Invalid login credential",
            "auth.token.rejected"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Session expired", "auth.token.rejected"),
    SESSION_REPLACED(HttpStatus.UNAUTHORIZED, "SESSION_REPLACED", "Account logged in elsewhere",
            "auth.token.rejected"),
    SESSION_KICKED(HttpStatus.UNAUTHORIZED, "SESSION_KICKED", "Account has been kicked out",
            "auth.token.rejected"),
    LOGIN_THROTTLED(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_THROTTLED",
            "Too many failed login attempts, try again later", "auth.login.throttled"),

    // --- 3000s: authorization -------------------------------------------------
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Missing permission",
            "auth.permission.denied"),
    ROLE_DENIED(HttpStatus.FORBIDDEN, "ROLE_DENIED", "Missing role", "auth.role.denied"),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED",
            "Only an administrator can do that", "auth.admin.required"),
    CANNOT_GRANT_EXCEEDING(HttpStatus.FORBIDDEN, "CANNOT_GRANT_EXCEEDING",
            "Cannot grant permissions you do not have", "auth.grant.exceeding"),
    CANNOT_MODIFY_EXCEEDING(HttpStatus.FORBIDDEN, "CANNOT_MODIFY_EXCEEDING",
            "Cannot modify a subject with permissions you do not have", "auth.modify.exceeding"),
    SELF_LOCKOUT(HttpStatus.FORBIDDEN, "SELF_LOCKOUT",
            "You cannot change your own role or disable yourself", "auth.self.lockout"),
    SELF_DELETE(HttpStatus.FORBIDDEN, "SELF_DELETE", "You cannot delete yourself", "auth.self.delete"),
    LAST_ADMIN(HttpStatus.FORBIDDEN, "LAST_ADMIN", "At least one active administrator must remain",
            "auth.admin.last"),
    BUILTIN_ROLE_IMMUTABLE(HttpStatus.FORBIDDEN, "BUILTIN_ROLE_IMMUTABLE",
            "Built-in roles cannot be modified", "role.builtin.rejected"),
    ROLE_IN_USE(HttpStatus.CONFLICT, "ROLE_IN_USE", "Role is still assigned to users",
            "role.delete.rejected"),

    // --- 4000s: resources -----------------------------------------------------
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found", null),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found", null),

    // --- 5000s: conflicts -----------------------------------------------------
    USERNAME_TAKEN(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username already exists", null),
    ROLE_CODE_TAKEN(HttpStatus.CONFLICT, "ROLE_CODE_TAKEN", "Role code already exists", null),

    // --- 9000s: system --------------------------------------------------------
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", null);

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final String auditEvent;

    BizCode(HttpStatus status, String code, String message, String auditEvent) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.auditEvent = auditEvent;
    }

    public HttpStatus status() {
        return status;
    }

    /** Stable machine identifier; the frontend localizes {@code error.<code>}. */
    public String code() {
        return code;
    }

    /** Default English message. Callers may override it when they can add detail. */
    public String message() {
        return message;
    }

    /** Audit event name, or {@code null} when this failure is not audited. */
    public String auditEvent() {
        return auditEvent;
    }
}
