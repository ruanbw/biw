package com.biw.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records a successful call as an audit event.
 * <p>
 * Applied by {@link AuditAspect}. Place it on the service method that performs the change, not on
 * the controller: the service knows the target id and whether the transaction committed.
 * <pre>
 * &#64;Audited(action = "user.create", targetType = "user")
 * public SysUser create(CreateUserRequest request) { ... }
 * </pre>
 * The record is written after the transaction commits, so a rolled-back operation leaves nothing
 * behind. Failures are not handled here — {@code GlobalExceptionHandler} records them as DENIED
 * through the same {@link BizCode} table.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** {@code domain.action}, e.g. {@code user.create}. */
    String action();

    /** The kind of subject changed, e.g. {@code user}. */
    String targetType() default "";

    /** Name of the method parameter that carries the target id. */
    String targetIdParam() default "id";

    /**
     * Read the target id from the returned value ({@code getId()}). Used by create, where the id
     * does not exist as a parameter when the method starts.
     */
    boolean targetIdFromResult() default false;

    /**
     * When true, the aspect reads the field changes collected by {@link AuditTrail}.
     * Use it for updates; a create or a delete records a plain snapshot instead.
     */
    boolean recordChanges() default false;
}
