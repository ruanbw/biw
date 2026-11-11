package com.biw.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.biw.common.api.Result;
import com.biw.common.audit.AuditEvent;
import com.biw.common.audit.AuditRecorder;
import com.biw.common.error.BizCode;
import com.biw.common.security.CurrentUser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Turns every failure into the same envelope.
 * <p>
 * Two rules make the response usable outside a browser:
 * <ul>
 *   <li>{@code errorCode} is stable and comes from {@link BizCode}; {@code message} is for humans
 *       and may be reworded or translated;</li>
 *   <li>a refused request is audited here, with the event name the code carries. That is what
 *       makes "a denial happened" observable, not just "the call failed".</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditRecorder audit;
    private final CurrentUser currentUser;
    private final Clock clock;

    public GlobalExceptionHandler(AuditRecorder audit, CurrentUser currentUser, Clock clock) {
        this.audit = audit;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        if (ex.getAuditEvent() != null) {
            recordDenied(ex.getAuditEvent(), Map.of("status", String.valueOf(ex.getStatus().value()),
                    "reason", String.valueOf(ex.getMessage())));
        } else if (ex.getStatus().is5xxServerError()) {
            log.error("business exception reported as {}", ex.getStatus().value(), ex);
        } else {
            log.debug("business exception: {}", ex.getMessage());
        }
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    /** {@code @Valid @RequestBody} failures. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(BizCode.VALIDATION_FAILED, message);
    }

    /** Form / query object binding failures. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(BizCode.VALIDATION_FAILED, message);
    }

    /** Method-level validation ({@code @Validated} on controller / service methods). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("; "));
        return build(BizCode.VALIDATION_FAILED, message);
    }

    /** Validation on non-controller beans ({@code @Validated} services, scheduled jobs). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(BizCode.VALIDATION_FAILED, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("unreadable request body: {}", ex.getMessage());
        return build(BizCode.MALFORMED_BODY, unreadableBodyMessage(ex));
    }

    /**
     * Names the field when Jackson rejected a value, typically an unknown enum constant such as
     * {@code "status": "MAYBE"}. Without this the client only learns that the body was unreadable.
     */
    private static String unreadableBodyMessage(HttpMessageNotReadableException ex) {
        if (!(ex.getCause() instanceof InvalidFormatException invalid) || !invalid.getTargetType().isEnum()) {
            return BizCode.MALFORMED_BODY.message();
        }
        // The path is rendered as `fully.qualified.Dto["status"]`; take the last quoted segment.
        // Jackson 3 no longer exposes the field name on the path reference itself.
        String field = fieldNameOf(invalid.getPathReference());
        if (field.isBlank()) {
            return BizCode.MALFORMED_BODY.message();
        }
        String allowed = Arrays.stream(invalid.getTargetType().getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return field + ": must be one of " + allowed;
    }

    private static String fieldNameOf(String pathReference) {
        int last = pathReference.lastIndexOf('"');
        if (last <= 0) {
            return "";
        }
        int first = pathReference.lastIndexOf('"', last - 1);
        return first < 0 ? "" : pathReference.substring(first + 1, last);
    }

    /** Body sent with a {@code Content-Type} no converter can read, e.g. {@code text/plain} on a JSON endpoint. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String contentType = ex.getContentType() == null ? "unknown" : ex.getContentType().toString();
        return build(BizCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type: " + contentType);
    }

    /**
     * The client refuses every media type we can produce. No body is written on purpose:
     * whatever we send would be refused for the same reason.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.debug("no acceptable response media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(BizCode.MISSING_PARAMETER, "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(BizCode.INVALID_PARAMETER, "Invalid parameter type: " + ex.getName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(BizCode.ENDPOINT_NOT_FOUND, "Endpoint not found: /" + ex.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(BizCode.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    // ---------------------------------------------------------------
    // Sa-Token
    // ---------------------------------------------------------------

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException ex) {
        BizCode code = switch (ex.getType()) {
            case NotLoginException.NOT_TOKEN -> BizCode.MISSING_CREDENTIAL;
            case NotLoginException.INVALID_TOKEN -> BizCode.INVALID_CREDENTIAL;
            case NotLoginException.TOKEN_TIMEOUT -> BizCode.SESSION_EXPIRED;
            case NotLoginException.BE_REPLACED -> BizCode.SESSION_REPLACED;
            case NotLoginException.KICK_OUT -> BizCode.SESSION_KICKED;
            default -> BizCode.SESSION_INVALID;
        };
        recordDenied(code.auditEvent(), Map.of("type", ex.getType(), "reason", code.code()));
        return build(code, code.message());
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException ex) {
        recordDenied(BizCode.ROLE_DENIED.auditEvent(), Map.of("role", String.valueOf(ex.getRole())));
        return build(BizCode.ROLE_DENIED, "Missing role: " + ex.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException ex) {
        recordDenied(BizCode.PERMISSION_DENIED.auditEvent(),
                Map.of("permission", String.valueOf(ex.getPermission())));
        return build(BizCode.PERMISSION_DENIED, "Missing permission: " + ex.getPermission());
    }

    // ---------------------------------------------------------------
    // Fallback
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return build(BizCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    private void recordDenied(String event, Map<String, String> detail) {
        if (event == null) {
            return;
        }
        Long actor = currentUser.idOrNull();
        audit.record(AuditEvent.denied(event, actor == null ? null : String.valueOf(actor),
                detail, MDC.get("requestId"), OffsetDateTime.now(clock)));
    }

    private static ResponseEntity<Result<Void>> build(BizCode code, String message) {
        return build(code.status(), code.code(), message);
    }

    private static ResponseEntity<Result<Void>> build(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status)
                .body(Result.fail(status.value(), errorCode, message == null ? status.getReasonPhrase() : message));
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
