package com.biw.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One entry of a select control. Deliberately not the entity itself: a picker needs an id, a
 * label and a disabled flag, and returning the whole row would ship fields the caller never
 * displays (and, for a user, would put the role lookup or the password column on the path).
 *
 * @param value       id submitted back to the API
 * @param label       primary text
 * @param description secondary text, e.g. the username behind a nickname
 * @param disabled    true when the row exists but cannot be chosen
 */
@Schema(description = "下拉选项")
public record OptionVO(
        @Schema(description = "提交值（用户或角色 ID）", example = "1") Long value,
        @Schema(description = "展示文案", example = "爱丽丝") String label,
        @Schema(description = "次要文案，如用户名或角色编码", example = "alice") String description,
        @Schema(description = "是否禁用；true 时前端应置灰且不可选", example = "false") boolean disabled
) {
}
