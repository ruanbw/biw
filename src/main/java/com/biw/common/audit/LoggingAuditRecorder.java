package com.biw.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes audit records to the {@code com.biw.security.audit} logger.
 * <p>
 * Kept on its own logger so a deployment can route it to a separate appender and alert on it: the
 * application log is not a place to look for security events, and this is the log that must
 * survive.
 * <p>
 * Convention for {@code action}: {@code domain.action}, e.g. {@code user.create} or
 * {@code role.assign-permissions}. Together with the outcome it forms the event name.
 * <p>
 * Never put a password or a session token into {@code detail}.
 */
@Component
public class LoggingAuditRecorder implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger("com.biw.security.audit");

    @Override
    public void record(AuditEvent event) {
        log.info("event={}.{} actor={} target={}:{} requestId={} detail={}",
                event.action(),
                event.outcome().name().toLowerCase(),
                orDash(event.actorId()),
                orDash(event.targetType()),
                orDash(event.targetId()),
                orDash(event.requestId() == null ? MDC.get("requestId") : event.requestId()),
                format(event.detail()));
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** One short line: a value with a newline would otherwise forge a log record. */
    private static String format(Map<String, String> detail) {
        if (detail == null || detail.isEmpty()) {
            return "-";
        }
        String single = detail.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","))
                .replace('\r', '_')
                .replace('\n', '_');
        return single.length() <= 300 ? single : single.substring(0, 300) + "...";
    }
}
