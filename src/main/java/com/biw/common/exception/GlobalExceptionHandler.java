package com.biw.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.biw.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Turns every exception into the {@link Result} envelope with a meaningful HTTP status.
 * <p>
 * Expected client errors (validation, auth, 404) are logged at debug/warn level;
 * unexpected ones are logged with the stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        log.debug("business exception: {}", ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage());
    }

    /** {@code @Valid @RequestBody} failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(ErrorCode.BAD_REQUEST, message.isBlank() ? ErrorCode.BAD_REQUEST.getMessage() : message);
    }

    /** Form / query object binding failures. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(ErrorCode.BAD_REQUEST, message.isBlank() ? ErrorCode.BAD_REQUEST.getMessage() : message);
    }

    /** Method-level validation ({@code @Validated} on controller / service methods). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("; "));
        return build(ErrorCode.BAD_REQUEST, message.isBlank() ? ErrorCode.BAD_REQUEST.getMessage() : message);
    }

    /** Validation on non-controller beans ({@code @Validated} services, scheduled jobs). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(ErrorCode.BAD_REQUEST, message.isBlank() ? ErrorCode.BAD_REQUEST.getMessage() : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("unreadable request body: {}", ex.getMessage());
        return build(ErrorCode.BAD_REQUEST, "请求体格式错误或无法解析");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(ErrorCode.BAD_REQUEST, "缺少必需参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.BAD_REQUEST, "参数类型错误: " + ex.getName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, "接口不存在: /" + ex.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    // ---------------------------------------------------------------
    // Sa-Token
    // ---------------------------------------------------------------

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException ex) {
        String message = switch (ex.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效";
            case NotLoginException.TOKEN_TIMEOUT -> "登录已过期";
            case NotLoginException.BE_REPLACED -> "账号已在别处登录";
            case NotLoginException.KICK_OUT -> "账号已被踢下线";
            default -> ErrorCode.UNAUTHORIZED.getMessage();
        };
        return build(ErrorCode.UNAUTHORIZED, message);
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException ex) {
        return build(ErrorCode.FORBIDDEN, "缺少角色: " + ex.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException ex) {
        return build(ErrorCode.FORBIDDEN, "缺少权限: " + ex.getPermission());
    }

    // ---------------------------------------------------------------
    // Fallback
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private static ResponseEntity<Result<Void>> build(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Result.fail(errorCode.getCode(), message));
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
