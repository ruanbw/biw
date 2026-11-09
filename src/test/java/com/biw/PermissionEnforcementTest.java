package com.biw;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.api.PageResult;
import com.biw.entity.rbac.PermissionCodes;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.service.RbacAuthorities;
import com.biw.service.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The chain the unit tests cannot cover: a real server, the Sa-Token context filter, the
 * interceptor, a real token, {@code StpInterfaceImpl}, and the annotation. Without this,
 * flipping {@code SaInterceptor.isAnnotation} off (or losing the {@code StpInterface} bean)
 * would leave every endpoint open to any logged-in user while the rest of the suite stays green.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PermissionEnforcementTest {

    private static final long USER_ID = 7L;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RbacAuthorities rbacAuthorities;

    /**
     * The test profile points at Redis, which is not running. Sessions live in memory instead;
     * {@code StpInterfaceImpl} is still the real bean and the interceptor is the real one.
     */
    @TestConfiguration
    static class InMemorySessionStore {

        @Bean
        @Primary
        SaTokenDao saTokenDao() {
            return new SaTokenDaoDefaultImpl();
        }
    }

    @Test
    void aLoggedInUserWithoutThePermissionGets403() throws Exception {
        String token = login();
        when(rbacAuthorities.permissionCodes(USER_ID)).thenReturn(List.of());

        assertThat(get("/api/users", token).statusCode()).isEqualTo(403);
        verify(userService, never()).listPage(anyLong(), anyLong(), any());
    }

    @Test
    void aUserHoldingThePermissionReachesTheHandler() throws Exception {
        String token = login();
        when(rbacAuthorities.permissionCodes(USER_ID))
                .thenReturn(List.of(PermissionCodes.USER_READ));
        when(userService.listPage(anyLong(), anyLong(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 1, 20, 0));

        assertThat(get("/api/users", token).statusCode()).isEqualTo(200);
        verify(userService).listPage(1, 20, null);
    }

    @Test
    void theAdminWildcardMatchesEveryPermission() throws Exception {
        String token = login();
        when(rbacAuthorities.permissionCodes(USER_ID))
                .thenReturn(List.of(PermissionCodes.ALL_WILDCARD));
        when(userService.listPage(anyLong(), anyLong(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 1, 20, 0));

        assertThat(get("/api/users", token).statusCode()).isEqualTo(200);
    }

    /**
     * Logs in through the real endpoint. The user lookup is mocked, but {@code StpUtil.login}
     * runs inside the request thread, so the token really is written to the session store.
     */
    private String login() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenAnswer(invocation -> {
            StpUtil.login(USER_ID);
            return new LoginVO(StpUtil.getTokenName(), StpUtil.getTokenValue(),
                    StpUtil.getTokenTimeout(), null);
        });

        HttpResponse<String> response = client.send(HttpRequest
                        .newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"username\":\"tester\",\"password\":\"test-password\"}",
                                StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.data.tokenValue");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        return client.send(HttpRequest
                        .newBuilder(URI.create("http://localhost:" + port + path))
                        .header("satoken", token)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
