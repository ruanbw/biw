package com.biw.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope for every JSON response.
 *
 * @param code    0 on success, business error code otherwise
 * @param message human readable message
 * @param data    payload, may be {@code null}
 */
public record Result<T>(int code, String message,
                        @JsonInclude(JsonInclude.Include.ALWAYS) T data) {

    public static final int SUCCESS = 0;

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
