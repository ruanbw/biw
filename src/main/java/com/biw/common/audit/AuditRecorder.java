package com.biw.common.audit;

/**
 * Where audit records go. The only extension point of the audit mechanism.
 * <p>
 * The template registers {@link LoggingAuditRecorder}. A deployment with a retention requirement
 * registers e.g. a {@code JdbcAuditRecorder} instead (or in addition, if the interface is
 * extended to accept a fan-out) and no service, controller or aspect changes.
 */
public interface AuditRecorder {

    void record(AuditEvent event);
}
