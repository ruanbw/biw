package com.biw.common.audit;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * One audit record: who did what to whom, and whether it succeeded.
 * <p>
 * Deliberately a plain value: the sink decides where it goes (log file, table, metrics). The
 * template ships a logging sink; a derived project that must retain records adds a JDBC sink and
 * registers it, without touching the call sites.
 */
public record AuditEvent(
        String action,
        Outcome outcome,
        String actorId,
        String targetType,
        String targetId,
        Map<String, String> detail,
        String requestId,
        OffsetDateTime occurredAt) {

    public enum Outcome {
        SUCCESS,
        DENIED
    }

    public static AuditEvent success(String action, String actorId, String targetType, String targetId,
                                     Map<String, String> detail, String requestId, OffsetDateTime at) {
        return new AuditEvent(action, Outcome.SUCCESS, actorId, targetType, targetId, detail, requestId, at);
    }

    public static AuditEvent denied(String action, String actorId, Map<String, String> detail,
                                    String requestId, OffsetDateTime at) {
        return new AuditEvent(action, Outcome.DENIED, actorId, null, null, detail, requestId, at);
    }
}
