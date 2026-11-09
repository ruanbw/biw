package com.biw.entity.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_.-]{2,31}$",
                message = "Username may contain letters, digits, '_', '.', '-' and must start with a letter or digit")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password,

        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER")
        String role,

        @Min(value = 0, message = "Status must be 0 or 1")
        @Max(value = 1, message = "Status must be 0 or 1")
        Integer status
) {
}
