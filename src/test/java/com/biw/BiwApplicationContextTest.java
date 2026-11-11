package com.biw;

import com.biw.common.audit.AuditRecorder;
import com.biw.common.audit.JdbcAuditRecorder;
import com.biw.common.audit.LoggingAuditRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full context (web server on a random port, MyBatis-Plus, Sa-Token,
 * Redis DAO auto-configuration) without contacting PostgreSQL or Redis:
 * both clients connect lazily, so a missing server does not fail startup.
 * <p>
 * The context is also the only place where the audit wiring can be observed: which sink is
 * primary depends on a property, and a mistake there silently stops all audit rows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BiwApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void auditWiringFollowsTheProfile() {
        // biw.audit.jdbc.enabled is false in the test profile: the log sink alone, no table.
        assertThat(context.getBeansOfType(JdbcAuditRecorder.class)).isEmpty();
        assertThat(context.getBean(AuditRecorder.class)).isInstanceOf(LoggingAuditRecorder.class);
    }
}
