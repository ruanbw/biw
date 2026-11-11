package com.biw;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A deployment that puts the admin bundle on its own origin sets
 * {@code biw.web.cors.allowed-origins}. The browser then sends an OPTIONS preflight before every
 * non-simple request, and that preflight carries no token by design. If the authentication
 * interceptor answers it with 401, the browser never sends the real request and the API looks
 * broken from the front end while every direct call works.
 * <p>
 * Runs against the real servlet stack (random port), because a MockMvc setup that skips the
 * filters would also skip the Sa-Token context and hide the behaviour under test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "biw.web.cors.allowed-origins=https://admin.example")
@ActiveProfiles("test")
class CorsPreflightTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void preflightIsAnsweredWithoutASession() throws Exception {
        HttpResponse<String> response = send(request("OPTIONS")
                .header("Origin", "https://admin.example")
                .header("Access-Control-Request-Method", "GET"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin"))
                .hasValue("https://admin.example");
    }

    @Test
    void aRealRequestStillNeedsASession() throws Exception {
        HttpResponse<String> response = send(request("GET")
                .header("Origin", "https://admin.example"));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpRequest.Builder request(String method) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/users"))
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
