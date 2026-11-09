package com.biw.config;

import cn.dev33.satoken.stp.StpInterface;
import com.biw.entity.user.SysUser;
import com.biw.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Roles come from the database on every check, so disabling or deleting a user
 * takes effect immediately even if the token is still valid.
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    public StpInterfaceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return List.of();
        }
        return List.of(user.getRole());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }
}
