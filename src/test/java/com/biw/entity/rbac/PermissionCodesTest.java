package com.biw.entity.rbac;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog in {@link PermissionCodes} and the seed in {@code sql/init.sql} must agree.
 * Without this check a new code could end up in an annotation but never in
 * {@code sys_permission}, leaving an endpoint no role can be granted.
 */
class PermissionCodesTest {

    private static final Path INIT_SQL =
            Path.of(System.getProperty("user.dir"), "sql", "init.sql");

    @Test
    void seededCatalogMatchesTheConstants() throws IOException {
        assertThat(INIT_SQL).as("sql/init.sql is the single source of the schema").exists();
        String sql = Files.readString(INIT_SQL, StandardCharsets.UTF_8);

        for (String code : PermissionCodes.ALL) {
            assertThat(sql)
                    .as("permission code %s must be inserted by sql/init.sql", code)
                    .contains("('" + code + "'");
        }
        // ADMIN and USER are the built-in roles the code reasons about.
        assertThat(sql).contains("('" + RoleCodes.ADMIN + "'");
        assertThat(sql).contains("('" + RoleCodes.USER + "'");
    }

    @Test
    void theCatalogHasNoDuplicates() {
        assertThat(PermissionCodes.ALL).doesNotHaveDuplicates().isNotEmpty();
    }
}
