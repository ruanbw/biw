package com.biw.controller;

import com.biw.common.exception.GlobalExceptionHandler;
import com.biw.support.TestDoubles;
import com.biw.service.RoleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RoleController(mock(RoleService.class)))
                .setControllerAdvice(TestDoubles.handler())
                .build();
    }

    @Test
    void rejectsLowercaseRoleCode() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"auditor\",\"name\":\"审计员\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Role code")));
    }

    @Test
    void rejectsBlankRoleName() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AUDITOR\",\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    void rejectsMissingPermissionCodes() throws Exception {
        mockMvc.perform(put("/api/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("permissionCodes")));
    }

    @Test
    void rejectsMoreThanTwoHundredPermissions() throws Exception {
        String codes = IntStream.rangeClosed(1, 201)
                .mapToObj(index -> "\"p" + index + "\"")
                .collect(Collectors.joining(","));

        mockMvc.perform(put("/api/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionCodes\":[" + codes + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("200")));
    }
}
