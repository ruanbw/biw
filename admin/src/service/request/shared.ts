import { localStg } from '@/utils/storage';
import type { RequestInstanceState } from './type';

/** Header name Sa-Token expects; the login response names it too. */
const DEFAULT_TOKEN_HEADER = 'satoken';

/** The request header carrying the token, or an empty object when logged out. */
export function getTokenHeader() {
  const token = localStg.get('token');
  const tokenName = localStg.get('tokenName') || DEFAULT_TOKEN_HEADER;

  return token ? { [tokenName]: token } : {};
}

export function showErrorMsg(state: RequestInstanceState, message: string) {
  /**
   * `window.$message` is mounted by the app, so it does not exist yet while the initial route
   * guard is fetching the session. Pushing onto the stack anyway would leave the message there
   * with no `onLeave` to ever remove it, and every later error with the same text would be
   * dropped as a duplicate. Return before touching the stack.
   */
  const messageApi = window.$message;

  if (!messageApi) return;

  if (!state.errMsgStack?.length) {
    state.errMsgStack = [];
  }

  const isExist = state.errMsgStack.includes(message);

  if (!isExist) {
    state.errMsgStack.push(message);

    messageApi.error(message, {
      onLeave: () => {
        state.errMsgStack = state.errMsgStack.filter(msg => msg !== message);

        setTimeout(() => {
          state.errMsgStack = [];
        }, 5000);
      }
    });
  }
}
