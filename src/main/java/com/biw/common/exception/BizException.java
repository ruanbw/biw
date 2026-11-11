package com.biw.common.exception;

import com.biw.common.error.BizCode;
import org.springframework.http.HttpStatus;

/**
 * Exception for expected, client-facing failures.
 * Handled by {@link GlobalExceptionHandler}, never logged as a server error.
 * <p>
 * Prefer {@link #BizException(BizCode)}: it carries a stable machine code and, when the code
 * names one, the audit event the handler should record. The {@code (HttpStatus, String)} form is
 * kept for the few places that cannot name a code, and it yields a null code rather than an
 * invented one.
 */
public class BizException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String auditEvent;

    public BizException(BizCode code) {
        this(code, code.message());
    }

    /** Same code, more detail in the human message (the offending values, usually). */
    public BizException(BizCode code, String message) {
        super(message);
        this.status = code.status();
        this.errorCode = code.code();
        this.auditEvent = code.auditEvent();
    }

    public BizException(HttpStatus status, String message) {
        this(status, null, message);
    }

    public BizException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.auditEvent = null;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Stable machine identifier, or {@code null} when built without a code. */
    public String getErrorCode() {
        return errorCode;
    }

    /** Audit event to record for this failure, or {@code null} when it is not audited. */
    public String getAuditEvent() {
        return auditEvent;
    }
}
