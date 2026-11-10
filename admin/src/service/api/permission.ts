import { request } from '../request';

/**
 * The permission catalog. Code-owned: rows are seeded by `sql/init.sql` from
 * `PermissionCodes`, so there is no write API.
 */
export function fetchGetPermissionList() {
  return request<Api.Permission.Permission[]>({ url: '/permissions', method: 'get' });
}
