package com.biw.entity.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname,

        @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER")
        String role,

        @Min(value = 0, message = "Status must be 0 or 1")
        @Max(value = 1, message = "Status must be 0 or 1")
        Integer status
) {
}
