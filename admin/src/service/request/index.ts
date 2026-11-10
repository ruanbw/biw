import type { AxiosResponse } from 'axios';
import { BACKEND_ERROR_CODE, createFlatRequest } from '@sa/axios';
import { getServiceBaseURL } from '@/utils/service';
import { useAuthStore } from '@/store/modules/auth';
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
      // 401 means the token is missing, expired, or the session was kicked out. There is no
      // refresh token to spend, so drop the local session and let the guard route to login.
      if (response.status === 401) {
        const authStore = useAuthStore();

        authStore.resetStore();
      }

      // Returning null hands the response to `onError`, which shows the backend message and
      // rejects, so the caller's `error` is set.
      return null;
    },
    onError(error) {
      const status = error.response?.status;

      // Already handled by `onBackendFail`: the redirect says more than a toast would.
      if (status === 401) {
        return;
      }

      let message = error.message;

      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || message;
      }

      showErrorMsg(request.state, message);
    }
  }
);
