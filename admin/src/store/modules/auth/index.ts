import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import { localStg } from '@/utils/storage';
import { fetchGetMe, fetchGetPermissions, fetchLogin, fetchLogout } from '@/service/api';
import { $t } from '@/locales';
import { useRouterPush } from '@/hooks/common/router';
import { SetupStoreId } from '@/enum';
import { useRouteStore } from '../route';
import { useTabStore } from '../tab';
import { clearAuthStorage, getToken } from './shared';

/**
 * Session state.
 *
 * `buttons` holds the permission codes returned by the backend instead of a separate
 * button list: biw has no button-level catalogue, and its permission check is already
 * the finest grain the API enforces.
 */
export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const route = useRoute();
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref('');

  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    userName: '',
    roles: [],
    buttons: []
  });

  /** Is login */
  const isLogin = computed(() => Boolean(token.value));

  /** Reset auth store */
  async function resetStore() {
    recordUserId();

    clearAuthStorage();

    // Not `$reset()`: the plugin would make it work, but this reset has an order that matters —
    // remember the previous user id, clear storage, clear state, then route and drop the tabs.
    token.value = '';
    Object.assign(userInfo, {
      userId: '',
      userName: '',
      roles: [],
      buttons: []
    });

    if (!route.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    routeStore.resetStore();
  }

  /** Record the user ID of the previous login session Used to compare with the current user ID on next login */
  function recordUserId() {
    if (!userInfo.userId) {
      return;
    }

    // Store current user ID locally for next login comparison
    localStg.set('lastLoginUserId', userInfo.userId);
  }

  /**
   * Check if current login user is different from previous login user If different, clear all tabs
   *
   * @returns {boolean} Whether to clear all tabs
   */
  function checkTabClear(): boolean {
    if (!userInfo.userId) {
      return false;
    }

    const lastLoginUserId = localStg.get('lastLoginUserId');

    // Clear all tabs if current user is different from previous user
    if (!lastLoginUserId || lastLoginUserId !== userInfo.userId) {
      localStg.remove('globalTabs');
      tabStore.clearTabs();

      localStg.remove('lastLoginUserId');
      return true;
    }

    localStg.remove('lastLoginUserId');
    return false;
  }

  /**
   * Login
   *
   * @param userName Username
   * @param password Password
   * @param [redirect=true] Whether to redirect after login. Default is `true`
   */
  async function login(userName: string, password: string, redirect = true) {
    startLoading();

    const { data: loginToken, error } = await fetchLogin(userName, password);

    if (!error) {
      const pass = await loginByToken(loginToken);

      if (pass) {
        // Check if the tab needs to be cleared
        const isClear = checkTabClear();
        let needRedirect = redirect;

        if (isClear) {
          // If the tab needs to be cleared, it means we don't need to redirect.
          needRedirect = false;
        }
        await redirectFromLogin(needRedirect);

        window.$notification?.success({
          title: $t('page.login.common.loginSuccess'),
          content: $t('page.login.common.welcomeBack', { userName: userInfo.userName }),
          duration: 4500
        });
      }
    }

    endLoading();
  }

  async function loginByToken(loginToken: Api.Auth.LoginToken) {
    // 1. stored in localStorage; later requests read both the header name and value from it
    localStg.set('token', loginToken.tokenValue);
    localStg.set('tokenName', loginToken.tokenName);

    token.value = loginToken.tokenValue;

    // 2. get user info and permissions
    const pass = await getUserInfo();

    if (!pass) {
      token.value = '';
    }

    return pass;
  }

  /**
   * Load the session: `/auth/me` for the profile, `/auth/permissions` for the codes.
   *
   * Roles come from the user's `roleCodes`; they drive menu filtering, while the
   * permission codes drive the in-page checks.
   */
  async function getUserInfo() {
    const [{ data: me, error }, { data: permissions, error: permissionError }] = await Promise.all([
      fetchGetMe(),
      fetchGetPermissions()
    ]);

    if (error || permissionError) {
      // The route guard reads the token from localStorage, so a memory-only reset would leave
      // the app thinking the stale session is still valid.
      clearAuthStorage();

      return false;
    }

    Object.assign(userInfo, {
      userId: String(me.id),
      userName: me.username,
      roles: me.roleCodes ?? [],
      buttons: permissions ?? []
    });

    return true;
  }

  async function initUserInfo() {
    const maybeToken = getToken();

    if (maybeToken) {
      token.value = maybeToken;
      const pass = await getUserInfo();

      if (!pass) {
        resetStore();
      }
    }
  }

  /** Ask the backend to drop the session, then clear local state. */
  async function logout() {
    await fetchLogout();
    await resetStore();
  }

  return {
    token,
    userInfo,
    isLogin,
    loginLoading,
    resetStore,
    login,
    logout,
    initUserInfo
  };
});
