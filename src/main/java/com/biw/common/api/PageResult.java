package com.biw.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Shape of every paginated list response.
 *
 * @param records rows of the current page
 * @param total   number of matching rows
 * @param page    current page number, starting at 1
 * @param size    page size
 * @param pages   total number of pages
 */
@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页数据") List<T> records,
        @Schema(description = "总记录数", example = "42") long total,
        @Schema(description = "当前页码，从 1 开始", example = "1") long page,
        @Schema(description = "每页条数", example = "20") long size,
        @Schema(description = "总页数", example = "3") long pages
) {
}
