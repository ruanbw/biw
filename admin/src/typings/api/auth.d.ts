declare namespace Api {
  /**
   * namespace Auth
   *
   * backend api module: "auth"
   */
  namespace Auth {
    /** `POST /api/auth/login` — the token is sent back in the header named `tokenName`. */
    interface LoginToken {
      /** Header name the token must travel in, e.g. "satoken" */
      tokenName: string;
      /** The token value */
      tokenValue: string;
      /** Seconds of validity, -1 for never expires */
      timeout: number;
      /** The logged-in user */
      user: Api.User.User;
    }

    /**
     * Local shape of the current session, assembled from `/api/auth/me` and
     * `/api/auth/permissions`. `buttons` holds permission codes; an admin gets the
     * single wildcard `*`.
     */
    interface UserInfo {
      userId: string;
      userName: string;
      roles: string[];
      buttons: string[];
    }
  }
}
