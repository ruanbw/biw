package com.biw.mapper.projection;

/**
 * One user-to-permission row: the codes a user holds through its assigned roles, regardless of
 * whether those roles are enabled. That is the set the delegation ceiling compares, so the page
 * of users uses the same query shape.
 */
public class UserPermissionCode {

    private Long userId;
    private String code;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
