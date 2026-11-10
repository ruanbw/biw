import { request } from '../request';

/**
 * Login
 *
 * @param username Username, case-insensitive
 * @param password Password
 */
export function fetchLogin(username: string, password: string) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/login',
    method: 'post',
    data: {
      username,
      password
    }
  });
}

/** Log out of the current session; calling it without a token also succeeds. */
export function fetchLogout() {
  return request<null>({
    url: '/auth/logout',
    method: 'post'
  });
}

/** The account behind the current token. */
export function fetchGetMe() {
  return request<Api.User.User>({ url: '/auth/me' });
}

/**
 * Effective permission codes of the current account.
 *
 * An admin answers `["*"]`, a disabled account answers an empty array.
 */
export function fetchGetPermissions() {
  return request<string[]>({ url: '/auth/permissions' });
}
