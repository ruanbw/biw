declare namespace Api {
  /**
   * namespace Permission
   *
   * backend api module: "permissions"
   */
  namespace Permission {
    interface Permission {
      id: number;
      /** The code referenced by annotations, e.g. "user:read" */
      code: string;
      name: string;
      description: string;
      createdAt: string;
    }
  }
}
