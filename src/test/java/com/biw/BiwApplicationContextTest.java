package com.biw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full context (web server on a random port, MyBatis-Plus, Sa-Token,
 * Redis DAO auto-configuration) without contacting PostgreSQL or Redis:
 * both clients connect lazily, so a missing server does not fail startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BiwApplicationContextTest {

    @Test
    void contextLoads() {
    }
}
