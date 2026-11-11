package com.biw.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envelope for every JSON response.
 *
 * @param code      HTTP status on failure, {@code 0} on success
 * @param errorCode stable machine identifier (see {@code BizCode}); {@code null} on success
 * @param message   human readable message
 * @param data      payload, may be {@code null}
 */
@Schema(description = "统一响应结构")
public record Result<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0") int code,
        @Schema(description = "稳定的机器错误码，成功时为 null", example = "USERNAME_TAKEN")
        String errorCode,
        @Schema(description = "提示信息，失败时为具体原因", example = "ok") String message,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "业务数据，失败时为 null") T data) {

    public static final int SUCCESS = 0;

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, null, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, null, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, null, message, null);
    }

    /**
     * The form the frontend and the tests rely on: {@code errorCode} does not change when a
     * message is reworded or translated.
     */
    public static <T> Result<T> fail(int code, String errorCode, String message) {
        return new Result<>(code, errorCode, message, null);
    }
}
