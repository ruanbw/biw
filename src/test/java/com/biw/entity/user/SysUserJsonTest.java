package com.biw.entity.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wire shape of a user: no password hash, enums by name, timestamps as ISO-8601.
 * Uses the whole context so the ObjectMapper is the one the application serves with.
 */
@SpringBootTest
@ActiveProfiles("test")
class SysUserJsonTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void hidesThePasswordHashAndUsesNamesAndIsoTimestamps() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("bob");
        user.setPasswordHash("$2a$12$not-for-the-client");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(OffsetDateTime.parse("2026-01-01T12:00:00+08:00"));

        String json = mapper.writeValueAsString(user);

        assertThat(json)
                .doesNotContain("passwordHash")
                .doesNotContain("not-for-the-client")
                .contains("\"status\":\"ENABLED\"")
                .contains("\"role\":\"USER\"")
                .contains("\"createdAt\":\"2026-01-01T12:00:00+08:00\"");
    }
}
