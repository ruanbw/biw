package com.biw.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.biw.entity.rbac.SysPermission;
import com.biw.mapper.SysPermissionMapper;
import com.biw.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;

    public PermissionServiceImpl(SysPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<SysPermission> listAll() {
        return permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getCode));
    }
}
