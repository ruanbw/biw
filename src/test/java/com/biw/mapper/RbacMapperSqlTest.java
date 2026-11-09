package com.biw.mapper;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The link-table statements are hand-written, and the project's tests never touch a database,
 * so this builds their SQL without executing it. It catches a broken {@code <script>} or
 * {@code <foreach>} that would otherwise only show up on the first request.
 */
@SpringBootTest
@ActiveProfiles("test")
class RbacMapperSqlTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void buildsTheBatchRoleLookupForAPageOfUsers() {
        String sql = sql("com.biw.mapper.SysUserRoleMapper.selectRoleCodesByUserIds",
                Map.of("userIds", List.of(1L, 2L)));

        assertThat(sql)
                .contains("ur.user_id IN")
                .contains("ORDER BY ur.user_id, r.code");
    }

    @Test
    void buildsTheActiveAdminLookupAndItsRowLock() {
        String sql = sql("com.biw.mapper.SysUserRoleMapper.selectActiveAdminUserIdsForUpdate", null);

        assertThat(sql)
                .contains("r.code = 'ADMIN'")
                .contains("ORDER BY u.id")
                .contains("FOR UPDATE OF u");
    }

    @Test
    void buildsTheAdminLookupForABatchOfUsers() {
        String sql = sql("com.biw.mapper.SysUserRoleMapper.selectActiveAdminUserIds",
                Map.of("userIds", List.of(1L, 2L, 3L)));

        assertThat(sql)
                .contains("r.code = 'ADMIN'")
                .contains("u.id IN");
    }

    @Test
    void buildsTheUnionOfPermissionsFromAllTheUsersRoles() {
        String sql = sql("com.biw.mapper.SysRolePermissionMapper.selectActivePermissionCodes",
                Map.of("userId", 1L));

        assertThat(sql)
                .contains("SELECT DISTINCT p.code")
                .contains("JOIN sys_role_permission")
                .contains("r.deleted = 0 AND r.status = 'ENABLED'");
    }

    @Test
    void countsOnlyExistingUsersForRoleDeletion() {
        String sql = sql("com.biw.mapper.SysUserRoleMapper.countUsersByRoleId",
                Map.of("roleId", 1L));

        assertThat(sql)
                .contains("JOIN sys_user u")
                .contains("u.deleted = 0")
                .contains("ur.role_id =");
    }

    @Test
    void buildsTheUnionOfPermissionsCarriedByRoles() {
        String sql = sql("com.biw.mapper.SysRolePermissionMapper.selectPermissionCodesByRoleIds",
                Map.of("roleIds", List.of(1L, 2L)));

        assertThat(sql)
                .contains("SELECT DISTINCT p.code")
                .contains("rp.role_id IN")
                .contains("ORDER BY p.code");
    }

    private String sql(String statementId, Object parameters) {
        MappedStatement statement = sqlSessionFactory.getConfiguration().getMappedStatement(statementId);
        return statement.getBoundSql(parameters).getSql();
    }
}
