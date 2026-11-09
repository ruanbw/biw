package com.biw.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envelope for every JSON response.
 *
 * @param code    0 on success, business error code otherwise
 * @param message human readable message
 * @param data    payload, may be {@code null}
 */
@Schema(description = "统一响应结构")
public record Result<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0") int code,
        @Schema(description = "提示信息，失败时为具体原因", example = "ok") String message,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "业务数据，失败时为 null") T data) {

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
