package com.biw.entity.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "批量删除请求")
public record BatchIdsRequest(
        @Schema(description = "要删除的用户 ID 列表，1-100 个", example = "[1, 2, 3]")
        @NotEmpty(message = "ids must not be empty")
        @Size(max = 100, message = "At most 100 users per request")
        List<@NotNull(message = "id must not be null") @Positive(message = "id must be positive") Long> ids
) {
}
