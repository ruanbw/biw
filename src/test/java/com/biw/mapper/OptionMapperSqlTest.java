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
 * The option queries are hand-written because they need {@code ILIKE} and a column list that
 * excludes sensitive fields. The suite never touches a database, so this builds their SQL and
 * pins the parts that matter: the logic-delete filter, the enabled-only search, the hydration
 * that deliberately keeps disabled rows, and the hard limit.
 */
@SpringBootTest
@ActiveProfiles("test")
class OptionMapperSqlTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void searchesUsersByUsernameOrNickname() {
        String sql = sql("com.biw.mapper.SysUserMapper.selectOptions",
                Map.of("pattern", "%ali%", "limit", 20));

        assertThat(sql)
                .contains("FROM sys_user")
                .contains("deleted = 0")
                .contains("status = 'ENABLED'")
                .contains("username ILIKE ?")
                .contains("nickname ILIKE ?")
                .contains("ORDER BY username, id")
                .contains("LIMIT ?");
    }

    @Test
    void omitsTheUserKeywordPredicateWhenThePickerOpens() {
        String sql = sql("com.biw.mapper.SysUserMapper.selectOptions", Map.of("limit", 20));

        assertThat(sql)
                .doesNotContain("ILIKE")
                .contains("status = 'ENABLED'");
    }

    @Test
    void hydratesUsersByIdWithoutDroppingDisabledOnes() {
        String sql = sql("com.biw.mapper.SysUserMapper.selectOptionsByIds",
                Map.of("ids", List.of(1L, 2L)));

        assertThat(sql)
                .contains("id IN")
                .contains("deleted = 0")
                .doesNotContain("status = 'ENABLED'")
                .contains("ORDER BY id");
    }

    @Test
    void searchesRolesByCodeOrName() {
        String sql = sql("com.biw.mapper.SysRoleMapper.selectOptions",
                Map.of("pattern", "%audit%", "limit", 20));

        assertThat(sql)
                .contains("FROM sys_role")
                .contains("deleted = 0")
                .contains("status = 'ENABLED'")
                .contains("code ILIKE ?")
                .contains("name ILIKE ?")
                .contains("ORDER BY code")
                .contains("LIMIT ?");
    }

    @Test
    void hydratesRolesByIdWithoutDroppingDisabledOnes() {
        String sql = sql("com.biw.mapper.SysRoleMapper.selectOptionsByIds",
                Map.of("ids", List.of(1L, 2L)));

        assertThat(sql)
                .contains("id IN")
                .contains("deleted = 0")
                .doesNotContain("status = 'ENABLED'")
                .contains("ORDER BY id");
    }

    private String sql(String statementId, Object parameters) {
        MappedStatement statement = sqlSessionFactory.getConfiguration().getMappedStatement(statementId);
        return statement.getBoundSql(parameters).getSql();
    }
}
