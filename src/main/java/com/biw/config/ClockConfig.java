package com.biw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The clock is a bean, not {@code OffsetDateTime.now()}.
 * <p>
 * Timestamps written by the services (created_at, updated_at) are part of what the tests assert,
 * and a static call cannot be controlled. A derived project that needs a different zone only
 * changes this bean.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
