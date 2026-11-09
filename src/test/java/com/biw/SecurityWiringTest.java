package com.biw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wiring of the login interceptor: /api/** needs a token, the login endpoint and the
 * API document do not. A wrong path pattern would silently make every endpoint public, so this
 * goes through a real server instead of a mocked one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityWiringTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int status(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path));
        if (body != null) {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .statusCode();
    }

    @Test
    void apiEndpointsRejectAnonymousRequests() throws Exception {
        assertThat(status("GET", "/api/auth/me", null)).isEqualTo(401);
        assertThat(status("GET", "/api/auth/permissions", null)).isEqualTo(401);
        assertThat(status("GET", "/api/users", null)).isEqualTo(401);
        assertThat(status("GET", "/api/roles", null)).isEqualTo(401);
        assertThat(status("PUT", "/api/roles/1/permissions", "{\"permissionCodes\":[]}")).isEqualTo(401);
        assertThat(status("GET", "/api/permissions", null)).isEqualTo(401);
        assertThat(status("POST", "/api/users/batch-delete", "{\"ids\":[1]}")).isEqualTo(401);
    }

    @Test
    void loginAndApiDocumentStayPublic() throws Exception {
        // 400 is the body validator, not the interceptor: the request reached the controller.
        assertThat(status("POST", "/api/auth/login", "{}")).isEqualTo(400);
        assertThat(status("GET", "/v3/api-docs", null)).isEqualTo(200);
    }
}
