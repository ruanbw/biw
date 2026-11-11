package com.biw.common.audit;

import com.biw.support.TestDoubles;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The database sink is a side effect of an operation that has already committed. It may lose a
 * record; it may not lose the request.
 * <p>
 * The row is the evidence, so the assertions are on the bound values and their order — a test
 * that only checks "update was called" passes with the actor id written into the target column.
 */
class JdbcAuditRecorderTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcAuditRecorder recorder = new JdbcAuditRecorder(jdbc);

    @Test
    void storesEveryFieldInItsColumn() {
        recorder.record(event());

        assertThat(boundValues()).containsExactly(
                "user.update",
                "SUCCESS",
                "7",
                "user",
                "42",
                "nickname=old->new",
                "req-1",
                TestDoubles.NOW_OFFSET);
    }

    @Test
    void storesAnAbsentOptionalFieldAsNullRatherThanAnEmptyString() {
        recorder.record(AuditEvent.success("role.delete", " ", "role", "", Map.of(), "", TestDoubles.NOW_OFFSET));

        assertThat(boundValues()).containsExactly(
                "role.delete", "SUCCESS", null, "role", null, null, null, TestDoubles.NOW_OFFSET);
    }

    @Test
    void flattensNewlinesInTheDetailSoALogLineCannotBeForged() {
        recorder.record(AuditEvent.success("user.update", "7", "user", "42",
                Map.of("nickname", "old\nnew\rinjected"), "req-1", TestDoubles.NOW_OFFSET));

        assertThat(String.valueOf(boundValues()[5])).isEqualTo("nickname=old_new_injected");
    }

    @Test
    void truncatesADetailThatWouldNotFitTheColumn() {
        String huge = "x".repeat(2000);
        recorder.record(AuditEvent.success("user.update", "7", "user", "42",
                Map.of("nickname", huge), "req-1", TestDoubles.NOW_OFFSET));

        String stored = String.valueOf(boundValues()[5]);
        assertThat(stored).hasSize(1003).endsWith("...");
    }

    @Test
    void doesNotPropagateAFailingSink() {
        when(jdbc.update(anyString(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("database is down"));

        assertThatCode(() -> recorder.record(event())).doesNotThrowAnyException();
    }

    private Object[] boundValues() {
        ArgumentCaptor<Object[]> values = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), values.capture());
        return values.getValue();
    }

    private static AuditEvent event() {
        return AuditEvent.success("user.update", "7", "user", "42",
                Map.of("nickname", "old->new"), "req-1", TestDoubles.NOW_OFFSET);
    }
}
