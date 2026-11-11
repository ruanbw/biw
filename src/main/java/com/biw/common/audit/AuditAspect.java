package com.biw.common.audit;

import com.biw.common.security.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Turns {@link Audited} into an audit record.
 * <p>
 * Two details matter more than the annotation itself:
 * <ul>
 *   <li>the record is written <strong>after the transaction commits</strong>. A service method
 *       that throws and rolls back must leave no audit line saying the change happened;</li>
 *   <li>a failure is not recorded here. {@code GlobalExceptionHandler} records DENIED with the
 *       event name from the same {@code BizCode} table, so a refused request still appears.</li>
 * </ul>
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditRecorder recorder;
    private final CurrentUser currentUser;
    private final Clock clock;

    public AuditAspect(AuditRecorder recorder, CurrentUser currentUser, Clock clock) {
        this.recorder = recorder;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable failure) {
            // The transaction will roll back; nothing was changed, so nothing is audited.
            AuditTrail.clear();
            throw failure;
        }

        AuditEvent event = build(joinPoint, audited, result);
        recordAfterCommit(event);
        return result;
    }

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private AuditEvent build(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        Map<String, String> detail = audited.recordChanges() ? AuditTrail.drain() : Map.of();
        return AuditEvent.success(
                audited.action(),
                actor(),
                audited.targetType(),
                targetId(joinPoint, audited, result),
                detail,
                MDC.get("requestId"),
                OffsetDateTime.now(clock));
    }

    private String targetId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        if (audited.targetIdFromResult()) {
            return idOf(result);
        }
        if (audited.targetIdParam().isEmpty() || !(joinPoint.getSignature() instanceof MethodSignature signature)) {
            return null;
        }
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < names.length && i < args.length; i++) {
            if (audited.targetIdParam().equals(names[i]) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    /** Reads {@code getId()} if the returned value has one; a missing id is not an error. */
    private static String idOf(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Object id = result.getClass().getMethod("getId").invoke(result);
            return id == null ? null : String.valueOf(id);
        } catch (ReflectiveOperationException noId) {
            log.debug("audited method returned {} which has no getId(); targetId left blank",
                    result.getClass().getName());
            return null;
        }
    }

    private String actor() {
        Long id = currentUser.idOrNull();
        return id == null ? null : String.valueOf(id);
    }

    private void recordAfterCommit(AuditEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recorder.record(event);
                }
            });
        } else {
            recorder.record(event);
        }
    }
}
