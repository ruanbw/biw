package com.biw.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * A permission the code knows about. Rows are seeded from {@link PermissionCodes}; there is
 * no write API, so the catalog cannot drift away from the annotations.
 */
@Schema(description = "权限信息")
@TableName("sys_permission")
public class SysPermission {

    @Schema(description = "权限 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "权限编码，注解里使用的就是它", example = "user:read")
    private String code;

    @Schema(description = "权限名称", example = "查看用户")
    private String name;

    @Schema(description = "权限说明", example = "分页查询、按 ID 查询用户")
    private String description;

    @Schema(description = "创建时间", example = "2026-01-01T12:00:00+08:00")
    private OffsetDateTime createdAt;

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
