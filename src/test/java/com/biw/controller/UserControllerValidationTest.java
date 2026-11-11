package com.biw.controller;

import com.biw.common.exception.GlobalExceptionHandler;
import com.biw.support.TestDoubles;
import com.biw.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(mock(UserService.class)))
                .setControllerAdvice(TestDoubles.handler())
                .build();
    }

    @Test
    void rejectsShortPasswordOnCreate() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("password")));
    }

    @Test
    void rejectsLowercaseRoleCodes() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"aaaaaaaa\",\"roleCodes\":[\"admin\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Role code")));
    }

    @Test
    void rejectsNullRoleCode() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"aaaaaaaa\",\"roleCodes\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Role code")));
    }

    @Test
    void rejectsMoreThanTwentyRoleCodes() throws Exception {
        String roleCodes = IntStream.rangeClosed(1, 21)
                .mapToObj(i -> "\"ROLE" + i + "\"")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"aaaaaaaa\",\"roleCodes\":["
                                + roleCodes + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("20")));
    }

    @Test
    void rejectsUnknownStatus() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"aaaaaaaa\",\"status\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                // Asserted on the stable code, not the wording: the message may be reworded or
                // translated, and Jackson 3 no longer reports the field name on the exception.
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_BODY"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void rejectsEmptyBatchIds() throws Exception {
        mockMvc.perform(post("/api/users/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("ids")));
    }
}
