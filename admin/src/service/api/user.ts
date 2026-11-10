import { request } from '../request';

/** Paginated users, newest first; `keyword` matches username or nickname. */
export function fetchGetUserList(params: Api.User.SearchParams) {
  return request<Api.User.Page<Api.User.User>>({
    url: '/users',
    method: 'get',
    params
  });
}

/**
 * Picker entries: enabled users only, or exactly the ids passed in `ids`
 * (disabled ones included, so a form can re-display an existing selection).
 */
export function fetchGetUserOptions(params: Api.User.OptionsParams) {
  return request<Api.User.Option[]>({
    url: '/users/options',
    method: 'get',
    params
  });
}

/** One user with `roleCodes`. */
export function fetchGetUser(id: number) {
  return request<Api.User.User>({ url: `/users/${id}`, method: 'get' });
}

export function fetchCreateUser(data: Api.User.CreateParams) {
  return request<Api.User.User>({
    url: '/users',
    method: 'post',
    data
  });
}

/** Null fields are left untouched; `roleCodes: null` keeps the current roles. */
export function fetchUpdateUser(id: number, data: Api.User.UpdateParams) {
  return request<Api.User.User>({
    url: `/users/${id}`,
    method: 'put',
    data
  });
}

/** Logical delete that also clears the role links and drops the session. */
export function fetchDeleteUser(id: number) {
  return request<null>({ url: `/users/${id}`, method: 'delete' });
}

/** At most 100 ids per request. */
export function fetchBatchDeleteUsers(data: Api.User.BatchIdsParams) {
  return request<null>({
    url: '/users/batch-delete',
    method: 'post',
    data
  });
}

/** Sets a new password and invalidates every session of that user. */
export function fetchResetUserPassword(id: number, data: Api.User.ResetPasswordParams) {
  return request<null>({
    url: `/users/${id}/reset-password`,
    method: 'post',
    data
  });
}
