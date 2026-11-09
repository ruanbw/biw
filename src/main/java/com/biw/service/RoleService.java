package com.biw.service;

import com.biw.common.api.OptionVO;
import com.biw.common.api.PageResult;
import com.biw.entity.rbac.SysRole;
import com.biw.entity.rbac.dto.AssignPermissionsRequest;
import com.biw.entity.rbac.dto.CreateRoleRequest;
import com.biw.entity.rbac.dto.UpdateRoleRequest;
import com.biw.entity.rbac.vo.RoleDetailVO;

import java.util.List;

/**
 * Role management. Deliberately not an {@code IService}, for the same reason as
 * {@link UserService}: the generic write methods would let a caller skip the built-in role rules.
 */
public interface RoleService {

    PageResult<SysRole> listPage(long page, long size, String keyword);

    /**
     * Entries for a role picker. A keyword search returns enabled roles only; {@code ids}
     * returns those rows as they are (disabled included) so an edit form can show the values it
     * already holds. Supplying {@code ids} ignores the keyword and the limit.
     *
     * @throws com.biw.common.exception.BizException 400 when more than 100 ids are supplied
     */
    List<OptionVO> listOptions(String keyword, int limit, List<Long> ids);

    /**
     * @throws com.biw.common.exception.BizException 404 when no such role exists
     */
    RoleDetailVO get(Long id);

    SysRole create(CreateRoleRequest request);

    SysRole update(Long id, UpdateRoleRequest request);

    void delete(Long id);

    /**
     * Replaces every permission of the role with the submitted codes.
     *
     * @throws com.biw.common.exception.BizException 400 when a code is not in the permission catalog
     */
    RoleDetailVO assignPermissions(Long id, AssignPermissionsRequest request);
}
