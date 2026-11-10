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
  if (!state.errMsgStack?.length) {
    state.errMsgStack = [];
  }

  const isExist = state.errMsgStack.includes(message);

  if (!isExist) {
    state.errMsgStack.push(message);

    window.$message?.error(message, {
      onLeave: () => {
        state.errMsgStack = state.errMsgStack.filter(msg => msg !== message);

        setTimeout(() => {
          state.errMsgStack = [];
        }, 5000);
      }
    });
  }
}
