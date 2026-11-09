package com.biw.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "角色信息")
@TableName("sys_role")
public class SysRole {

    @Schema(description = "角色 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "角色编码，创建后不可修改", example = "AUDITOR")
    private String code;

    @Schema(description = "角色名称", example = "审计员")
    private String name;

    @Schema(description = "角色说明", example = "只能查看用户和角色")
    private String description;

    @Schema(description = "角色状态", example = "ENABLED")
    private RoleStatus status;

    @Schema(description = "是否内置角色；内置角色不可删除、不可禁用，权限不可编辑", example = "false")
    private Boolean builtIn;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
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
