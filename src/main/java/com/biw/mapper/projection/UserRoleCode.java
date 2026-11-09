package com.biw.mapper.projection;

/**
 * One user-to-role row, used to attach role codes to a page of users with a single query
 * instead of one query per user.
 */
public class UserRoleCode {

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
