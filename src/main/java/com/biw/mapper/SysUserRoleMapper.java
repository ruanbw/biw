package com.biw.mapper;

import com.biw.mapper.projection.UserRoleCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * The user-to-role link table. It has a composite primary key, which MyBatis-Plus's
 * {@code BaseMapper} cannot address, so the few statements it needs are written here.
 */
public interface SysUserRoleMapper {

    /** Role codes as assigned, for display; disabled roles are included. */
    @Select("""
            SELECT r.code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /**
     * Role codes that currently grant anything: the user and the role must both be active.
     * This is what authorization decisions use, so disabling either takes effect at once.
     */
    @Select("""
            SELECT r.code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            JOIN sys_user u ON u.id = ur.user_id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0 AND r.status = 'ENABLED'
              AND u.deleted = 0 AND u.status = 'ENABLED'
            ORDER BY r.code
            """)
    List<String> selectActiveRoleCodes(@Param("userId") Long userId);

    /** Role ids as assigned, used to resolve which permissions an account can hold. */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} ORDER BY role_id")
    List<Long> selectRoleIds(@Param("userId") Long userId);

    /** Role codes for a page of users, so listing does not issue one query per row. */
    @Select("""
            <script>
            SELECT ur.user_id, r.code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE ur.user_id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY ur.user_id, r.code
            </script>
            """)
    List<UserRoleCode> selectRoleCodesByUserIds(@Param("userIds") Collection<Long> userIds);

    @Select("""
            SELECT COUNT(*)
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0
            WHERE ur.role_id = #{roleId}
            """)
    long countActiveUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * Active admins, locked until the transaction ends. Two requests deleting or demoting an
     * admin must see each other, otherwise both can see two admins and leave none. ORDER BY id
     * gives every caller the same lock order, so they cannot deadlock.
     */
    @Select("""
            SELECT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.deleted = 0 AND u.status = 'ENABLED'
              AND r.code = 'ADMIN' AND r.deleted = 0 AND r.status = 'ENABLED'
            ORDER BY u.id
            FOR UPDATE OF u
            """)
    List<Long> selectActiveAdminUserIdsForUpdate();

    /** Which of the given users are active admins right now, without locking. */
    @Select("""
            <script>
            SELECT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.deleted = 0 AND u.status = 'ENABLED'
              AND r.code = 'ADMIN' AND r.deleted = 0 AND r.status = 'ENABLED'
              AND u.id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectActiveAdminUserIds(@Param("userIds") Collection<Long> userIds);

    @Insert("""
            INSERT INTO sys_user_role (user_id, role_id)
            VALUES (#{userId}, #{roleId})
            ON CONFLICT DO NOTHING
            """)
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
