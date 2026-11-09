package com.biw.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "用户信息")
@TableName("sys_user")
public class SysUser {

    @Schema(description = "用户 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名", example = "alice")
    private String username;

    @JsonIgnore
    private String passwordHash;

    @Schema(description = "昵称", example = "爱丽丝")
    private String nickname;

    @Schema(description = "角色编码列表（含已禁用角色，用于展示）；实际生效权限见 /api/auth/permissions",
            example = "[\"USER\"]")
    @TableField(exist = false)
    private List<String> roleCodes;

    @Schema(description = "状态", example = "ENABLED")
    private UserStatus status;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    @Schema(description = "创建时间", example = "2026-01-01T12:00:00+08:00")
    private OffsetDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-01-01T12:00:00+08:00")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
