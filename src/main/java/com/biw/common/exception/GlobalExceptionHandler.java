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
 * Expected client errors (validation, auth, 404) are logged at debug level;
 * unexpected ones are logged with the stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        log.debug("business exception: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getMessage());
    }

    /** {@code @Valid @RequestBody} failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase() : message);
    }

    /** Form / query object binding failures. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase() : message);
    }

    /** Method-level validation ({@code @Validated} on controller / service methods). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase() : message);
    }

    /** Validation on non-controller beans ({@code @Validated} services, scheduled jobs). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase() : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("unreadable request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter type: " + ex.getName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Endpoint not found: /" + ex.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    // ---------------------------------------------------------------
    // Sa-Token
    // ---------------------------------------------------------------

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException ex) {
        String message = switch (ex.getType()) {
            case NotLoginException.NOT_TOKEN -> "Missing login credential";
            case NotLoginException.INVALID_TOKEN -> "Invalid login credential";
            case NotLoginException.TOKEN_TIMEOUT -> "Session expired";
            case NotLoginException.BE_REPLACED -> "Account logged in elsewhere";
            case NotLoginException.KICK_OUT -> "Account has been kicked out";
            default -> HttpStatus.UNAUTHORIZED.getReasonPhrase();
        };
        return build(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException ex) {
        return build(HttpStatus.FORBIDDEN, "Missing role: " + ex.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException ex) {
        return build(HttpStatus.FORBIDDEN, "Missing permission: " + ex.getPermission());
    }

    // ---------------------------------------------------------------
    // Fallback
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    private static ResponseEntity<Result<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Result.fail(status.value(), message));
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
