package com.biw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biw.entity.user.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Options for a select control: enabled users whose username or nickname matches.
     * {@code pattern} is a ready-to-bind LIKE pattern (already escaped); {@code null} means the
     * picker just opened and no keyword predicate is added.
     * <p>
     * Hand-written because MyBatis-Plus has no case-insensitive LIKE, and because the column list
     * is part of the contract here: a picker must not read {@code password_hash}.
     */
    @Select("""
            <script>
            SELECT id, username, nickname, status
            FROM sys_user
            WHERE deleted = 0
              AND status = 'ENABLED'
            <if test="pattern != null">
              AND (username ILIKE #{pattern} OR nickname ILIKE #{pattern})
            </if>
            ORDER BY username, id
            LIMIT #{limit}
            </script>
            """)
    List<SysUser> selectOptions(@Param("pattern") String pattern, @Param("limit") int limit);

    /**
     * Options for the ids a form already selected. Disabled users are included on purpose: the
     * form has to show what it is holding, and the status lets the UI grey it out instead of
     * dropping the value. No {@code status} filter here, unlike the search query.
     */
    @Select("""
            <script>
            SELECT id, username, nickname, status
            FROM sys_user
            WHERE deleted = 0
              AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<SysUser> selectOptionsByIds(@Param("ids") Collection<Long> ids);
}
