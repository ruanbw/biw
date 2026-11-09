package com.biw.entity.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchIdsRequest(
        @NotEmpty(message = "ids must not be empty")
        @Size(max = 100, message = "At most 100 users per request")
        List<@NotNull(message = "id must not be null") @Positive(message = "id must be positive") Long> ids
) {
}
