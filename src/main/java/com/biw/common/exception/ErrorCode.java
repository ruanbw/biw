package com.biw.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Business error codes. HTTP status mirrors {@code code} where possible.
 */
public enum ErrorCode {

    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "请求参数错误"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "无权限访问"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "资源不存在"),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    CONFLICT(409, HttpStatus.CONFLICT, "资源冲突"),
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
