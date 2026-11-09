package com.biw.entity.rbac;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog in {@link PermissionCodes} and the seed in sql/init.sql must agree.
 * Without this check a new code could end up in an annotation but never in
 * {@code sys_permission}, leaving an endpoint no role can be granted.
 */
class PermissionCodesTest {

    @Test
    void seededCatalogMatchesTheConstants() throws IOException {
        Path initScript = Path.of(System.getProperty("user.dir"), "sql", "init.sql");
        assertThat(initScript).exists();
        String sql = Files.readString(initScript, StandardCharsets.UTF_8);

        for (String code : PermissionCodes.ALL) {
            assertThat(sql).contains("('" + code + "'");
        }
        // ADMIN and USER are the built-in roles the code reasons about.
        assertThat(sql).contains("('" + RoleCodes.ADMIN + "'");
        assertThat(sql).contains("('" + RoleCodes.USER + "'");
    }
}
