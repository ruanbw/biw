package com.biw.config;

import com.biw.entity.rbac.RoleStatus;
import com.biw.entity.user.UserStatus;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The status columns are native PostgreSQL enums, which reject the {@code varchar} parameter the
 * stock {@code EnumTypeHandler} sends. Two things have to stay true, and both are wiring rather
 * than code: the property must be picked up, and the registry must resolve a Java enum class
 * without entity metadata, because wrapper parameters (the {@code set(...)} path) are bound by
 * their runtime class only.
 * <p>
 * MyBatis-Plus wraps whatever is configured in a {@code CompositeEnumTypeHandler}, so the
 * assertion is on the SQL it produces, not on the handler class.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnumTypeHandlerWiringTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private <E extends Enum<E>> TypeHandler<E> handlerFor(Class<E> type) {
        TypeHandler<E> handler = sqlSessionFactory.getConfiguration()
                .getTypeHandlerRegistry().getTypeHandler(type);
        assertThat(handler).as("handler for %s", type.getSimpleName()).isNotNull();
        return handler;
    }

    @Test
    void enumParametersAreSentAsUntypedLiterals() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);

        handlerFor(UserStatus.class).setParameter(statement, 3, UserStatus.DISABLED, null);

        // setString would bind a varchar and the server would refuse the assignment.
        verify(statement).setObject(3, "DISABLED", Types.OTHER);
    }

    @Test
    void enumColumnsAreReadBackByName() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("status")).thenReturn("DISABLED");

        assertThat(handlerFor(RoleStatus.class).getResult(resultSet, "status"))
                .isEqualTo(RoleStatus.DISABLED);
    }
}
