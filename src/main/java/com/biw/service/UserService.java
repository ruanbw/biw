package com.biw.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.biw.common.api.PageResult;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;

import java.util.List;

public interface UserService extends IService<SysUser> {

    LoginVO login(LoginRequest request);

    SysUser me();

    PageResult<SysUser> listPage(long page, long size, String keyword);

    SysUser create(CreateUserRequest request);

    SysUser update(Long id, UpdateUserRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void resetPassword(Long id, ResetPasswordRequest request);
}
