package com.biw.convert;

import com.biw.entity.user.SysUser;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private final UserConverter converter = Mappers.getMapper(UserConverter.class);

    @Test
    void fillsDefaultsForOmittedFields() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", null, null, null), "bob", "hash");

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getNickname()).isEmpty();
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getDeleted()).isZero();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getId()).isNull();
    }

    @Test
    void keepsExplicitFieldsAndTrimsNickname() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", "  Bob  ", "ADMIN", 0), "bob", "hash");

        assertThat(user.getNickname()).isEqualTo("Bob");
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.getStatus()).isZero();
    }

    @Test
    void appliesOnlyPresentUpdateFields() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", "Bob", "USER", 1), "bob", "hash");
        user.setId(7L);

        converter.applyUpdate(new UpdateUserRequest("  Bobby  ", null, 0), user);

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getNickname()).isEqualTo("Bobby");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getStatus()).isZero();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
    }
}
