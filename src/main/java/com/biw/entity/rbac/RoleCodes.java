package com.biw.entity.rbac;

/**
 * Codes of the built-in roles created by {@code sql/init.sql}. Custom roles are data and
 * have no constants here; only the two the code reasons about are named.
 */
public final class RoleCodes {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private RoleCodes() {
    }
}
