package com.biw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biw.entity.rbac.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * Options for a select control: enabled roles whose code or name matches. {@code pattern} is
     * a ready-to-bind LIKE pattern (already escaped); {@code null} adds no keyword predicate.
     */
    @Select("""
            <script>
            SELECT id, code, name, status
            FROM sys_role
            WHERE deleted = 0
              AND status = 'ENABLED'
            <if test="pattern != null">
              AND (code ILIKE #{pattern} OR name ILIKE #{pattern})
            </if>
            ORDER BY code
            LIMIT #{limit}
            </script>
            """)
    List<SysRole> selectOptions(@Param("pattern") String pattern, @Param("limit") int limit);

    /**
     * Options for the ids a form already selected, including disabled roles so an existing
     * assignment can still be displayed (and greyed out) instead of disappearing.
     */
    @Select("""
            <script>
            SELECT id, code, name, status
            FROM sys_role
            WHERE deleted = 0
              AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<SysRole> selectOptionsByIds(@Param("ids") Collection<Long> ids);
}
