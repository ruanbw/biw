/**
 * Username
 *
 * Mirrors the backend constraint (CreateUserRequest): 3-32 characters, must start with a letter
 * or digit, and may contain letters, digits, '_', '.', '-'. The backend stores it lowercase.
 */
export const REG_USER_NAME = /^[A-Za-z0-9][A-Za-z0-9_.-]{2,31}$/;

/** Phone reg */
export const REG_PHONE =
  /^[1](([3][0-9])|([4][01456789])|([5][012356789])|([6][2567])|([7][0-8])|([8][0-9])|([9][012356789]))[0-9]{8}$/;

/**
 * Password reg
 *
 * Mirrors the backend constraint: 8-64 characters. No character-class rule is imposed, because
 * the backend does not impose one either.
 */
export const REG_PWD = /^.{8,64}$/;

/** Email reg */
export const REG_EMAIL = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/;

/** Six digit code reg */
export const REG_CODE_SIX = /^\d{6}$/;

/** Four digit code reg */
export const REG_CODE_FOUR = /^\d{4}$/;

/** Url reg */
export const REG_URL =
  /(((^https?:(?:\/\/)?)(?:[-;:&=+$,\w]+@)?[A-Za-z0-9.-]+(?::\d+)?|(?:www.|[-;:&=+$,\w]+@)[A-Za-z0-9.-]+)((?:\/[+~%/.\w-_]*)?\??(?:[-+=&;%@.\w_]*)#?(?:[\w]*))?)$/;
