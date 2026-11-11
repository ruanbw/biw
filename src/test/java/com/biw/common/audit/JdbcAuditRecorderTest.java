package com.biw.common.audit;

import com.biw.support.TestDoubles;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The database sink is a side effect of an operation that has already committed. It may lose a
 * record; it may not lose the request.
 */
class JdbcAuditRecorderTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcAuditRecorder recorder =
            new JdbcAuditRecorder(jdbc, TestDoubles.clock(), Duration.ofDays(365));

    @Test
    void storesTheEventAsOneRow() {
        recorder.record(event());

        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    void doesNotPropagateAFailingSink() {
        when(jdbc.update(anyString(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("database is down"));

        assertThatCode(() -> recorder.record(event())).doesNotThrowAnyException();
    }

    private static AuditEvent event() {
        return AuditEvent.success("user.update", "7", "user", "42",
                Map.of("nickname", "old->new"), "req-1", TestDoubles.NOW_OFFSET);
    }
}
