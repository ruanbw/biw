package com.biw.common.exception;

import cn.dev33.satoken.exception.NotPermissionException;
import com.biw.common.api.Result;
import com.biw.support.TestDoubles;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 403 contract the API document promises: a missing permission code is an envelope with
 * code 403 and the missing code in the message, not a bare 500.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = TestDoubles.handler();

    @Test
    void notPermissionBecomesA403Envelope() {
        ResponseEntity<Result<Void>> response = handler.handleNotPermission(
                new NotPermissionException("user:read", "login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
        assertThat(response.getBody().message()).contains("user:read");
    }
}
