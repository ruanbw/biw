package com.biw;

import com.biw.entity.rbac.PermissionCodes;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that cross a language boundary and therefore cannot be caught by the compiler.
 * <p>
 * The frontend declares a permission code per route; the backend defines the catalog. A code
 * that exists on one side only fails silently at runtime — a menu that never appears, or an
 * endpoint nobody can be granted — which is exactly the failure this template must not teach.
 */
class TemplateIntegrityTest {

    private static final Path ROUTES =
            Path.of(System.getProperty("user.dir"), "admin", "src", "router", "elegant", "routes.ts");

    private static final Pattern PERMISSIONS = Pattern.compile("permissions:\\s*\\[([^\\]]*)\\]");

    @Test
    void everyRoutePermissionExistsInTheBackendCatalog() throws IOException {
        if (!Files.exists(ROUTES)) {
            // A backend-only checkout has nothing to compare. Ask JUnit to report it as skipped:
            // returning silently made the check disappear from the report, and a check nobody can
            // see is a check that is already off.
            Assumptions.abort("admin/ is not present in this checkout; cross-language check skipped");
        }

        List<String> declared = new ArrayList<>();
        Matcher matcher = PERMISSIONS.matcher(Files.readString(ROUTES, StandardCharsets.UTF_8));
        while (matcher.find()) {
            for (String raw : matcher.group(1).split(",")) {
                String code = raw.trim().replace("'", "").replace("\"", "");
                if (!code.isEmpty()) {
                    declared.add(code);
                }
            }
        }

        assertThat(declared)
                .as("every meta.permissions code in admin/src/router/elegant/routes.ts must exist in PermissionCodes.ALL")
                .isNotEmpty()
                .allSatisfy(code -> assertThat(PermissionCodes.ALL).contains(code));
    }

    @Test
    void theCatalogIsNotAccidentallyEmpty() {
        assertThat(PermissionCodes.ALL).doesNotHaveDuplicates().isNotEmpty();
    }
}
