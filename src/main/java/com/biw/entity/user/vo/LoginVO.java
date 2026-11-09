package com.biw.entity.user.vo;

import com.biw.entity.user.SysUser;

public record LoginVO(String tokenName, String tokenValue, long timeout, SysUser user) {
}
