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
 * The OpenAPI document must be reachable without a token and must carry the Chinese
 * descriptions added to controllers/DTOs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiDocsTest {

    @LocalServerPort
    private int port;

    @Test
    void exposesChineseDescriptionsAndTokenScheme() throws Exception {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"title\":\"biw 接口文档\"")
                .contains("\"name\":\"认证\"")
                .contains("\"name\":\"用户管理\"")
                .contains("\"summary\":\"分页查询用户\"")
                .contains("\"description\":\"用户名，不区分大小写\"")
                .contains("密码：8 到 64 个字符")
                .contains("\"type\":\"apiKey\"")
                // status and role are named, not numeric
                .contains("\"enum\":[\"ENABLED\",\"DISABLED\"]")
                .contains("\"enum\":[\"ADMIN\",\"USER\"]")
                .contains("/api/users/{id}")
                // internal fallback endpoint and sensitive columns must stay out of the contract
                .doesNotContain("\"/error\"")
                .doesNotContain("passwordHash")
                .doesNotContain("password_hash");
    }

    @Test
    void servesSwaggerUi() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html"))
                .GET()
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
    }
}
