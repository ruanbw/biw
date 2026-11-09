package com.biw.entity.user.vo;

import com.biw.entity.user.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录结果")
public record LoginVO(
        @Schema(description = "token 名称，也是请求头的名称", example = "satoken") String tokenName,
        @Schema(description = "token 值，放入请求头 satoken", example = "1c8f1e0e-3b0d-4b7f-9a51-8f0e1f2a3b4c") String tokenValue,
        @Schema(description = "token 有效期（秒），-1 表示永久有效", example = "604800") long timeout,
        @Schema(description = "登录用户信息") SysUser user
) {
}
