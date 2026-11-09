package com.biw.convert;

import com.biw.entity.user.SysUser;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * DTO &lt;-&gt; entity mapping for {@link SysUser}. Implemented by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * Builds a new user row. {@code username} and {@code passwordHash} are passed in already
     * normalized/encoded because they are computed from the request by the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "nickname", source = "request.nickname", qualifiedByName = "trimToEmpty")
    @Mapping(target = "role", source = "request.role", defaultValue = "USER")
    @Mapping(target = "status", source = "request.status", defaultValue = "1")
    @Mapping(target = "deleted", constant = "0")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    SysUser toEntity(CreateUserRequest request, String username, String passwordHash);

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
    @Mapping(target = "nickname", source = "nickname", qualifiedByName = "trim")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    void applyUpdate(UpdateUserRequest request, @MappingTarget SysUser user);

    @Named("trimToEmpty")
    default String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
