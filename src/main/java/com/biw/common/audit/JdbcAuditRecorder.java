package com.biw.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes audit records to {@code sys_audit_log}.
 * <p>
 * The log line from {@link LoggingAuditRecorder} is what an operator greps while an incident is
 * open; a row is what is still there a year later, when the log has rotated away and the
 * application process could have rewritten it. Enabled with {@code biw.audit.jdbc.enabled}
 * because it needs the table; {@code application-prod.yml} turns it on.
 * <p>
 * A failing sink never fails the request it describes: the business transaction has already
 * committed, and losing the request as well as the record would be the worse outcome. The
 * warning it logs is the signal to fix the sink.
 */
@Component
@ConditionalOnProperty(prefix = "biw.audit.jdbc", name = "enabled", havingValue = "true")
public class JdbcAuditRecorder implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(JdbcAuditRecorder.class);

    private static final String INSERT = """
            INSERT INTO sys_audit_log
                (action, outcome, actor_id, target_type, target_id, detail, request_id, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration retention;

    public JdbcAuditRecorder(JdbcTemplate jdbc, Clock clock,
                             @Value("${biw.audit.jdbc.retention:365d}") Duration retention) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.retention = retention;
    }

    @Override
    public void record(AuditEvent event) {
        try {
            jdbc.update(INSERT,
                    event.action(),
                    event.outcome().name(),
                    event.actorId(),
                    blankToNull(event.targetType()),
                    blankToNull(event.targetId()),
                    format(event.detail()),
                    blankToNull(event.requestId()),
                    event.occurredAt());
        } catch (RuntimeException ex) {
            log.warn("audit record {}.{} could not be stored: {}", event.action(),
                    event.outcome().name().toLowerCase(), ex.getMessage());
        }
    }

    /**
     * Keeps the table bounded. A record is evidence for as long as the retention period says and
     * not forever — an unbounded table is its own outage.
     */
    @Scheduled(cron = "${biw.audit.jdbc.retention-cron:0 30 3 * * *}")
    public void purgeExpired() {
        try {
            int deleted = jdbc.update("DELETE FROM sys_audit_log WHERE occurred_at < ?",
                    OffsetDateTime.now(clock).minus(retention));
            if (deleted > 0) {
                log.info("purged {} audit record(s) older than {}", deleted, retention);
            }
        } catch (RuntimeException ex) {
            log.warn("audit retention sweep failed: {}", ex.getMessage());
        }
    }

    /** One short line, the same shape the log sink writes; a newline in a value would forge one. */
    private static String format(Map<String, String> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        String single = detail.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","))
                .replace('\r', '_')
                .replace('\n', '_');
        return single.length() <= 1000 ? single : single.substring(0, 1000) + "...";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
