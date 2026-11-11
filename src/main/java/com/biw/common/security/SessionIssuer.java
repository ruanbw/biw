package com.biw.common.security;

/**
 * Issues a session on a successful login. Split from {@link CurrentUser} because logging in
 * happens before there is a caller to ask about.
 */
public interface SessionIssuer {

    /** @param userId the account that just proved its password */
    IssuedToken login(Long userId);

    record IssuedToken(String tokenName, String tokenValue, long timeoutSeconds) {
    }
}
