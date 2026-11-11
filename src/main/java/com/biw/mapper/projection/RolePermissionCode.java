package com.biw.mapper.projection;

/**
 * One role-to-permission row, used to attach permission codes to a page of roles with a single
 * query instead of one query per role. The list needs them to tell the caller which rows it is
 * allowed to modify.
 */
public class RolePermissionCode {

    private Long roleId;
    private String code;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
