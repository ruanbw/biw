package com.biw.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * The role-to-permission link table. Like {@link SysUserRoleMapper} it has a composite
 * primary key, so it is addressed with explicit statements.
 */
public interface SysRolePermissionMapper {

    /** Every permission code assigned to the role, used by the role detail endpoint. */
    @Select("""
            SELECT p.code
            FROM sys_role_permission rp
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.code
            """)
    List<String> selectPermissionCodes(@Param("roleId") Long roleId);

    /**
     * The union of the permission codes granted by the user's active roles. Deduplicated
     * because two roles may carry the same permission.
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            JOIN sys_user u ON u.id = ur.user_id
            JOIN sys_role_permission rp ON rp.role_id = r.id
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0 AND r.status = 'ENABLED'
              AND u.deleted = 0 AND u.status = 'ENABLED'
            ORDER BY p.code
            """)
    List<String> selectActivePermissionCodes(@Param("userId") Long userId);

    /**
     * The union of the permission codes carried by the given roles. Used to stop a delegated
     * user manager from assigning a role that holds more than the manager itself has.
     */
    @Select("""
            <script>
            SELECT DISTINCT p.code
            FROM sys_role_permission rp
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE rp.role_id IN
            <foreach collection="roleIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY p.code
            </script>
            """)
    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Insert("""
            INSERT INTO sys_role_permission (role_id, permission_id)
            VALUES (#{roleId}, #{permissionId})
            ON CONFLICT DO NOTHING
            """)
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
