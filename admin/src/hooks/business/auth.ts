import { useAuthStore } from '@/store/modules/auth';

/**
 * Permission checks for in-page controls.
 *
 * Codes come from `GET /api/auth/permissions`, which answers `["*"]` for an admin, so every
 * check passes for them without listing each code.
 */
export function useAuth() {
  const authStore = useAuthStore();

  function hasAuth(codes: string | string[]) {
    if (!authStore.isLogin) {
      return false;
    }

    const granted = authStore.userInfo.buttons;

    if (granted.includes('*')) {
      return true;
    }

    if (typeof codes === 'string') {
      return granted.includes(codes);
    }

    return codes.some(code => granted.includes(code));
  }

  return {
    hasAuth
  };
}
