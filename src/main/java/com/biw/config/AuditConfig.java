package com.biw.config;

import com.biw.common.audit.AuditRecorder;
import com.biw.common.audit.JdbcAuditRecorder;
import com.biw.common.audit.LoggingAuditRecorder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Chooses where an audit record goes.
 * <p>
 * The log sink is always present. The database sink joins it when
 * {@code biw.audit.jdbc.enabled} is true, rather than replacing it: during an incident the first
 * thing anyone does is grep the log, and the row is what is left after the log has rotated.
 * Both write the same {@link com.biw.common.audit.AuditEvent}, so no call site changes.
 */
@Configuration
public class AuditConfig {

    @Bean
    @Primary
    public AuditRecorder auditRecorder(LoggingAuditRecorder logging,
                                       ObjectProvider<JdbcAuditRecorder> jdbcRecorder) {
        JdbcAuditRecorder jdbc = jdbcRecorder.getIfAvailable();
        if (jdbc == null) {
            return logging;
        }
        return event -> {
            jdbc.record(event);
            logging.record(event);
        };
    }
}
