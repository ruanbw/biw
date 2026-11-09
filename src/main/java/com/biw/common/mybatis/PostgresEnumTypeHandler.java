package com.biw.common.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Binds a Java enum to a native PostgreSQL enum column by constant name.
 * <p>
 * The stock {@code EnumTypeHandler} writes with {@code setString}, which pgjdbc sends as
 * {@code varchar}; PostgreSQL refuses to assign that to an enum column
 * ({@code column "status" is of type user_status but expression is of type character varying}).
 * {@link Types#OTHER} makes pgjdbc send the label untyped, so the server resolves it against the
 * column's enum type. Reads need no special handling: the label comes back as a string.
 * <p>
 * Wired as {@code mybatis-plus.configuration.default-enum-type-handler} so it also covers
 * parameters bound through wrappers, not only entity fields.
 */
public class PostgresEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    public PostgresEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("An enum type is required");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter.name(), Types.OTHER);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getString(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getString(columnIndex));
    }

    private E toEnum(String label) {
        return label == null ? null : Enum.valueOf(type, label);
    }
}
