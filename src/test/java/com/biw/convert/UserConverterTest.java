package com.biw.convert;

import com.biw.entity.user.SysUser;
import com.biw.entity.user.UserStatus;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import com.biw.support.TestDoubles;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private final UserConverter converter = Mappers.getMapper(UserConverter.class);

    @Test
    void fillsDefaultsForOmittedFields() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", null, null, null), "bob", "hash", TestDoubles.NOW_OFFSET);

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getNickname()).isEmpty();
        assertThat(user.getRoleCodes()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ENABLED);
        assertThat(user.getDeleted()).isZero();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getId()).isNull();
    }

    @Test
    void takesTheUsernameFromTheParameterNotFromTheRequest() {
        // The service normalizes the username before mapping; a raw uppercase value must not win.
        SysUser user = converter.toEntity(
                new CreateUserRequest("Bob", "raw-password", null, null, null), "bob", "hash", TestDoubles.NOW_OFFSET);

        assertThat(user.getUsername()).isEqualTo("bob");
    }

    @Test
    void keepsExplicitFieldsAndTrimsNickname() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", "  Bob  ", List.of("ADMIN"),
                        UserStatus.DISABLED),
                "bob", "hash", TestDoubles.NOW_OFFSET);

        assertThat(user.getNickname()).isEqualTo("Bob");
        // Role links are written by the service; the transient request list must not leak in.
        assertThat(user.getRoleCodes()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void appliesOnlyPresentUpdateFields() {
        SysUser user = converter.toEntity(
                new CreateUserRequest("bob", "raw-password", "Bob", List.of("USER"),
                        UserStatus.ENABLED),
                "bob", "hash", TestDoubles.NOW_OFFSET);
        user.setId(7L);

        converter.applyUpdate(new UpdateUserRequest("  Bobby  ", List.of("ADMIN"),
                UserStatus.DISABLED), user, TestDoubles.NOW_OFFSET);

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getNickname()).isEqualTo("Bobby");
        assertThat(user.getRoleCodes()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
    }
}
