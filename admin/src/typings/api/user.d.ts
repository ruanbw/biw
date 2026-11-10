declare namespace Api {
  /**
   * namespace User
   *
   * backend api module: "users"
   */
  namespace User {
    /** Stored by name in the `user_status` enum. */
    type Status = 'ENABLED' | 'DISABLED';

    interface User {
      id: number;
      username: string;
      nickname: string;
      /** Role codes, including disabled roles; effective permissions come from /auth/permissions */
      roleCodes?: string[];
      status: Status;
      createdAt: string;
      updatedAt: string;
    }

    /** `Result.data` of every paginated endpoint. */
    interface Page<T> {
      records: T[];
      total: number;
      /** 1-based */
      page: number;
      size: number;
      pages: number;
    }

    interface SearchParams {
      page: number;
      size: number;
      /** Matches username or nickname */
      keyword?: string;
    }

    interface CreateParams {
      /** 3-32 chars, letters/digits/_/./-, unique and case-insensitive */
      username: string;
      /** 8-64 chars */
      password: string;
      nickname?: string;
      /** Omitted means [USER]; empty array means no role */
      roleCodes?: string[];
      status?: Status;
    }

    /** Only non-null fields are written; `roleCodes: null` keeps the current roles. */
    interface UpdateParams {
      nickname?: string;
      roleCodes?: string[] | null;
      status?: Status;
    }

    interface ResetPasswordParams {
      newPassword: string;
    }

    interface BatchIdsParams {
      /** 1-100 ids */
      ids: number[];
    }

    /** `OptionVO`: value is the id submitted back to the API. */
    interface Option {
      value: number;
      label: string;
      /** Secondary text; for a user it is the username, or null when the nickname is empty. */
      description: string | null;
      disabled: boolean;
    }

    interface OptionsParams {
      keyword?: string;
      limit?: number;
      /** Re-display already chosen ids, including disabled ones; max 100 */
      ids?: number[];
    }
  }
}
