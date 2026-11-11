import type { AxiosResponse } from 'axios';
import { createFlatRequest } from '@sa/axios';
import { getServiceBaseURL } from '@/utils/service';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { getTokenHeader, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

/**
 * Every endpoint lives under `/api`.
 *
 * The dev proxy strips its `/proxy-default` prefix and forwards the rest, so appending `/api`
 * here yields `/proxy-default/api/auth/login` in the browser and `/api/auth/login` at the
 * backend. In production the base URL is empty, so the same suffix simply targets the current
 * origin.
 */
const apiBaseURL = `${baseURL}/api`;

/**
 * Drop the local session after a 401 — but only when there was a session to drop.
 *
 * There is no refresh token to spend, so a 401 on an authenticated request means the session is
 * gone: clear the token, the user info and the cached tabs, then let the guard route to login.
 * A 401 with no token is a rejected login attempt; that needs the backend message on screen, not
 * a redirect away from the page the user is typing on.
 *
 * @returns whether a session was actually cleared
 */
async function handleUnauthorized(): Promise<boolean> {
  if (!Object.keys(getTokenHeader()).length) {
    return false;
  }

  await useAuthStore().resetStore();

  return true;
}

/**
 * The localized text for a backend `errorCode`, or `null` when this build has no message for it.
 *
 * vue-i18n answers the key itself when the message is missing, so comparing against the key is
 * the only way to tell "translated" from "unknown code" (a backend newer than the bundle).
 */
function localizeErrorCode(errorCode: string): string | null {
  const key = `error.${errorCode}` as App.I18n.I18nKey;
  const localized = $t(key);

  return localized === key ? null : localized;
}

/**
 * The single HTTP client for the biw backend.
 *
 * Contract notes, all of them different from the template default:
 * - the envelope carries `message`, not `msg`;
 * - success is `code === 0`; a failure carries the HTTP status as `code`;
 * - the token travels in the header named by the login response (`satoken`) with no
 *   `Bearer` prefix, and there is no refresh token — a 401 means the session is gone.
 */
export const request = createFlatRequest(
  {
    baseURL: apiBaseURL
  },
  {
    defaultState: {
      errMsgStack: []
    } as RequestInstanceState,
    transform(response: AxiosResponse<App.Service.Response<any>>) {
      return response.data.data;
    },
    async onRequest(config) {
      Object.assign(config.headers, getTokenHeader());

      return config;
    },
    isBackendSuccess(response) {
      return response.data.code === Number(import.meta.env.VITE_SERVICE_SUCCESS_CODE);
    },
    async onBackendFail(response) {
      // Only reached for a 2xx answer carrying a failing business code. That is not how this
      // backend reports failures - it answers real HTTP status codes - so a 401 realistically
      // arrives in `onError`. Keep both paths covered.
      if (response.status === 401) {
        await handleUnauthorized();
      }

      // Returning null hands the response to `onError`, which shows the backend message and
      // rejects, so the caller's `error` is set.
      return null;
    },
    async onError(error) {
      const status = error.response?.status;

      // Token expired or kicked out: clear the session and let the guard redirect, which says
      // more than a toast would. A 401 without a token is a failed login, handled below.
      if (status === 401 && (await handleUnauthorized())) {
        return;
      }

      /**
       * Real 4xx/5xx answers carry the backend's own `Result`: `errorCode` is the stable
       * identifier, `message` its English default. Prefer the localized text for the code,
       * then the backend message, then the axios text (which only repeats the HTTP status).
       */
      const backend = error.response?.data;
      const localized = backend?.errorCode ? localizeErrorCode(backend.errorCode) : null;
      const message = localized || backend?.message || error.message;

      showErrorMsg(request.state, message);
    }
  }
);
