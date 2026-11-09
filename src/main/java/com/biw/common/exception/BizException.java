package com.biw.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for expected, client-facing failures.
 * Handled by {@link GlobalExceptionHandler}, never logged as a server error.
 */
public class BizException extends RuntimeException {

    private final HttpStatus status;

    public BizException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BizException(HttpStatus status) {
        this(status, status.getReasonPhrase());
    }

    public BizException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
