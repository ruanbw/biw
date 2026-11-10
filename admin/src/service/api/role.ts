import { request } from '../request';

export function fetchGetRoleList(params: Api.Role.SearchParams) {
  return request<Api.Role.Page>({
    url: '/roles',
    method: 'get',
    params
  });
}

/**
 * Picker entries. `user:write` may call this too, because the user form needs
 * role names.
 */
export function fetchGetRoleOptions(params: Api.Role.OptionsParams) {
  return request<Api.Role.Option[]>({
    url: '/roles/options',
    method: 'get',
    params
  });
}

/** One role plus the permission codes it grants; ADMIN answers `["*"]`. */
export function fetchGetRole(id: number) {
  return request<Api.Role.Detail>({ url: `/roles/${id}`, method: 'get' });
}

export function fetchCreateRole(data: Api.Role.CreateParams) {
  return request<Api.Role.Role>({
    url: '/roles',
    method: 'post',
    data
  });
}

/** Null fields are left untouched; the code itself cannot be changed. */
export function fetchUpdateRole(id: number, data: Api.Role.UpdateParams) {
  return request<Api.Role.Role>({
    url: `/roles/${id}`,
    method: 'put',
    data
  });
}

/** Built-in roles and roles still held by a user are refused. */
export function fetchDeleteRole(id: number) {
  return request<null>({ url: `/roles/${id}`, method: 'delete' });
}

/** Replaces the whole permission set; an empty array revokes everything. */
export function fetchAssignRolePermissions(id: number, data: Api.Role.AssignPermissionsParams) {
  return request<Api.Role.Detail>({
    url: `/roles/${id}/permissions`,
    method: 'put',
    data
  });
}
