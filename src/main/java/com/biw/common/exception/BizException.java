package com.biw.common.exception;

/**
 * Exception for expected, client-facing failures.
 * Handled by {@link GlobalExceptionHandler}, never logged as a server error.
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }
}
