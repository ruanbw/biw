package com.biw.convert;

import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserStatus;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.OffsetDateTime;

/**
 * DTO &lt;-&gt; entity mapping for {@link SysUser}. Implemented by MapStruct at compile time.
 * <p>
 * The timestamps are passed in rather than read here: {@code OffsetDateTime.now()} inside a
 * mapping expression is a static dependency that no test can control.
 */
@Mapper(componentModel = "spring", imports = {UserStatus.class})
public interface UserConverter {

    /**
     * Builds a new user row. {@code username} and {@code passwordHash} are passed in already
     * normalized/encoded because they are computed from the request by the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "nickname", source = "request.nickname", qualifiedByName = "trimToEmpty")
    @Mapping(target = "roleCodes", ignore = true)
    @Mapping(target = "status", source = "request.status", defaultExpression = "java(UserStatus.ENABLED)")
    @Mapping(target = "deleted", constant = "0")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    SysUser toEntity(CreateUserRequest request, String username, String passwordHash, OffsetDateTime now);

    /**
     * Copies the present fields of {@code request} onto an existing row and stamps
     * {@code updatedAt}. Fields the endpoint must not change are left untouched.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "nickname", source = "request.nickname", qualifiedByName = "trim")
    @Mapping(target = "roleCodes", ignore = true)
    @Mapping(target = "updatedAt", source = "now")
    void applyUpdate(UpdateUserRequest request, @MappingTarget SysUser user, OffsetDateTime now);

    @Named("trimToEmpty")
    default String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
