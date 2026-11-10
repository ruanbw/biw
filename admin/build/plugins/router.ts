import type { RouteMeta } from 'vue-router';
import type { RouteKey } from '@elegant-router/types';
import ElegantVueRouter from '@elegant-router/vue/vite';

/**
 * Menu metadata for the routes this admin ships.
 *
 * It lives here rather than in the generated `src/router/elegant/routes.ts`, which is
 * rewritten on every dev-server start.
 *
 * `permissions` is matched against `GET /api/auth/permissions`; the wildcard `*` (what an
 * ADMIN receives) matches everything, so no route has to enumerate ADMIN.
 */
const routeMetaRecord: Record<string, Partial<RouteMeta>> = {
  home: { icon: 'mdi:monitor-dashboard', order: 1 },
  system: { icon: 'carbon:settings', order: 2 },
  system_user: { icon: 'ic:round-manage-accounts', permissions: ['user:read'] },
  system_role: { icon: 'ic:round-admin-panel-settings', permissions: ['role:read'] },
  system_permission: { icon: 'ic:round-key', permissions: ['permission:read'] }
};

export function setupElegantRouter() {
  return ElegantVueRouter({
    layouts: {
      base: 'src/layouts/base-layout/index.vue',
      blank: 'src/layouts/blank-layout/index.vue'
    },
    routePathTransformer(routeName, routePath) {
      const key = routeName as RouteKey;

      // biw only exposes username/password login: no SMS code, self-registration,
      // self-service password reset, or WeChat binding.
      if (key === 'login') {
        return '/login/:module(pwd-login)?';
      }

      return routePath;
    },
    onRouteMetaGen(routeName) {
      const key = routeName as RouteKey;

      const constantRoutes: RouteKey[] = ['login', '403', '404', '500'];

      const meta: Partial<RouteMeta> = {
        title: key,
        i18nKey: `route.${key}` as App.I18n.I18nKey
      };

      if (constantRoutes.includes(key)) {
        meta.constant = true;
      }

      return { ...meta, ...routeMetaRecord[key] };
    }
  });
}
