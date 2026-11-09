package com.biw.entity.rbac;

import java.util.List;

/**
 * Every permission the code checks. Annotations reference these constants, and the seeded
 * rows in {@code sql/init.sql} must match them; {@code PermissionCodesTest} fails when a
 * code is missing from the migrations, so an endpoint cannot silently end up with a
 * permission no role can ever be granted.
 * <p>
 * Role names are deliberately absent from annotations: a role is a bundle of these codes.
 */
public final class PermissionCodes {

    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_WRITE = "role:write";
    public static final String PERMISSION_READ = "permission:read";

    /** What Sa-Token matches against every code; {@code ADMIN} gets exactly this list. */
    public static final String ALL_WILDCARD = "*";

    /** The catalog, in the order the SQL seed lists it. */
    public static final List<String> ALL = List.of(
            USER_READ,
            USER_WRITE,
            ROLE_READ,
            ROLE_WRITE,
            PERMISSION_READ);

    private PermissionCodes() {
    }
}
