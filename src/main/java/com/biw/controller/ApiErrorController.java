package com.biw.controller;

import com.biw.common.api.Result;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ensures container-level errors also use the {@link Result} envelope.
 * Hidden from the OpenAPI document: it is an internal fallback, not a callable API.
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Result<Void>> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status instanceof Integer value ? value : HttpStatus.INTERNAL_SERVER_ERROR.value();

        HttpStatus httpStatus = HttpStatus.resolve(code);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            code = httpStatus.value();
        }
        return ResponseEntity.status(httpStatus)
                .body(Result.fail(code, httpStatus.getReasonPhrase()));
    }
}
