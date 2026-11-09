package com.biw.service;

import com.biw.entity.rbac.SysPermission;

import java.util.List;

/**
 * The permission catalog. It is read-only on purpose: rows come from {@code PermissionCodes}
 * through the seed in {@code sql/init.sql}, so a code and its row cannot drift apart.
 */
public interface PermissionService {

    /** Every permission, ordered by code. */
    List<SysPermission> listAll();
}
