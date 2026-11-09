package com.biw.entity.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(max = 64, message = "Username must not exceed 64 characters")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
        String password
) {
}
