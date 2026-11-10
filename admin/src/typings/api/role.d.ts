declare namespace Api {
  /**
   * namespace Role
   *
   * backend api module: "roles"
   */
  namespace Role {
    type Status = 'ENABLED' | 'DISABLED';

    interface Role {
      id: number;
      /** Uppercase, immutable after creation */
      code: string;
      name: string;
      description: string;
      status: Status;
      /** Built-in roles cannot be disabled, deleted, or have their permissions edited */
      builtIn: boolean;
      createdAt: string;
      updatedAt: string;
    }

    /** `Result.data` of `GET /api/roles/{id}` and `PUT /api/roles/{id}/permissions`. */
    interface Detail {
      role: Role;
      /** ADMIN answers `["*"]` */
      permissionCodes: string[];
      /** Users currently holding this role, disabled users included */
      userCount: number;
    }

    interface Page {
      records: Role[];
      total: number;
      page: number;
      size: number;
      pages: number;
    }

    interface SearchParams {
      page: number;
      size: number;
      /** Matches role code or name */
      keyword?: string;
    }

    interface CreateParams {
      /** Uppercase letter start, then A-Z, 0-9 and '_', max 32 */
      code: string;
      name: string;
      description?: string;
    }

    /** Only non-null fields are written; the code itself cannot change. */
    interface UpdateParams {
      name?: string;
      description?: string;
      status?: Status;
    }

    interface AssignPermissionsParams {
      /** Replaces the whole set; an empty array revokes every permission */
      permissionCodes: string[];
    }

    /** `OptionVO`, shared with users: `value` is the role id, `description` the role code. */
    interface Option {
      value: number;
      label: string;
      /** The role code — what a user form actually submits. */
      description: string;
      disabled: boolean;
    }

    interface OptionsParams {
      keyword?: string;
      limit?: number;
      ids?: number[];
    }
  }
}
