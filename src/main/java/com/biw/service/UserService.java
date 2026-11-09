package com.biw.service;

import com.biw.common.api.OptionVO;
import com.biw.common.api.PageResult;
import com.biw.entity.user.dto.CreateUserRequest;
import com.biw.entity.user.dto.LoginRequest;
import com.biw.entity.user.dto.ResetPasswordRequest;
import com.biw.entity.user.dto.UpdateUserRequest;
import com.biw.entity.user.vo.LoginVO;
import com.biw.entity.user.SysUser;

import java.util.List;

/**
 * User management. Deliberately not an {@code IService}: exposing the generic save/remove/update
 * methods would let any caller skip the admin rules the service enforces.
 */
public interface UserService {

    LoginVO login(LoginRequest request);

    SysUser me();

    /**
     * @throws com.biw.common.exception.BizException 404 when no such user exists
     */
    SysUser get(Long id);

    PageResult<SysUser> listPage(long page, long size, String keyword);

    /**
     * Entries for a user picker. A keyword search returns enabled users only; {@code ids}
     * returns those rows as they are (disabled included) so an edit form can show the values it
     * already holds. Supplying {@code ids} ignores the keyword and the limit.
     *
     * @throws com.biw.common.exception.BizException 400 when more than 100 ids are supplied
     */
    List<OptionVO> listOptions(String keyword, int limit, List<Long> ids);

    SysUser create(CreateUserRequest request);

    SysUser update(Long id, UpdateUserRequest request);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void resetPassword(Long id, ResetPasswordRequest request);
}
