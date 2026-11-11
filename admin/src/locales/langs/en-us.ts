const local: App.I18n.Schema = {
  system: {
    title: 'biw Admin',
    updateTitle: 'System Version Update Notification',
    updateContent: 'A new version of the system has been detected. Do you want to refresh the page immediately?',
    updateConfirm: 'Refresh immediately',
    updateCancel: 'Later'
  },
  common: {
    action: 'Action',
    add: 'Add',
    addSuccess: 'Add Success',
    backToHome: 'Back to home',
    batchDelete: 'Batch Delete',
    cancel: 'Cancel',
    close: 'Close',
    check: 'Check',
    selectAll: 'Select All',
    expandColumn: 'Expand Column',
    columnSetting: 'Column Setting',
    config: 'Config',
    confirm: 'Confirm',
    delete: 'Delete',
    deleteSuccess: 'Delete Success',
    confirmDelete: 'Are you sure you want to delete?',
    edit: 'Edit',
    warning: 'Warning',
    error: 'Error',
    index: 'Index',
    keywordSearch: 'Please enter keyword',
    logout: 'Logout',
    logoutConfirm: 'Are you sure you want to log out?',
    lookForward: 'Coming soon',
    modify: 'Modify',
    modifySuccess: 'Modify Success',
    noData: 'No Data',
    operate: 'Operate',
    pleaseCheckValue: 'Please check whether the value is valid',
    refresh: 'Refresh',
    reset: 'Reset',
    search: 'Search',
    switch: 'Switch',
    tip: 'Tip',
    trigger: 'Trigger',
    update: 'Update',
    updateSuccess: 'Update Success',
    userCenter: 'User Center',
    yesOrNo: {
      yes: 'Yes',
      no: 'No'
    }
  },
  request: {
    logout: 'Logout user after request failed',
    logoutMsg: 'User status is invalid, please log in again',
    logoutWithModal: 'Pop up modal after request failed and then log out user',
    logoutWithModalMsg: 'User status is invalid, please log in again',
    refreshToken: 'The requested token has expired, refresh the token',
    tokenExpired: 'The requested token has expired'
  },
  /** One message per backend `BizCode.errorCode`; must stay in step with zh-cn.ts. */
  error: {
    VALIDATION_FAILED: 'The submitted values failed validation. Check them and try again',
    MALFORMED_BODY: 'The request body is malformed and cannot be read',
    UNSUPPORTED_MEDIA_TYPE: 'Unsupported content type',
    MISSING_PARAMETER: 'A required parameter is missing',
    INVALID_PARAMETER: 'A request parameter has the wrong type',
    PAGE_TOO_DEEP: 'That page is too deep; narrow the search instead',
    IDS_OUT_OF_RANGE: 'Select between 1 and 100 items per request',
    USERNAME_REQUIRED: 'Username must not be blank',
    PASSWORD_TOO_LONG: 'Password must not exceed 72 bytes',
    ROLE_NAME_REQUIRED: 'Role name must not be blank',
    UNKNOWN_ROLE: 'That role no longer exists',
    UNKNOWN_PERMISSION: 'That permission no longer exists',
    ENDPOINT_NOT_FOUND: 'Endpoint not found',
    METHOD_NOT_ALLOWED: 'Method not allowed',
    NOT_ACCEPTABLE: 'No acceptable response format is available',
    BAD_CREDENTIALS: 'Incorrect username or password',
    SESSION_INVALID: 'Your session is no longer valid. Please sign in again',
    MISSING_CREDENTIAL: 'The login credential is missing. Please sign in again',
    INVALID_CREDENTIAL: 'The login credential is invalid. Please sign in again',
    SESSION_EXPIRED: 'Your session has expired. Please sign in again',
    SESSION_REPLACED: 'This account signed in elsewhere. Please sign in again',
    SESSION_KICKED: 'This account was signed out. Please sign in again',
    LOGIN_THROTTLED: 'Too many failed sign-in attempts. Try again later',
    PERMISSION_DENIED: 'You do not have the required permission',
    ROLE_DENIED: 'You do not have the required role',
    ADMIN_REQUIRED: 'Only an administrator can do that',
    CANNOT_GRANT_EXCEEDING: 'You cannot grant permissions you do not have',
    CANNOT_MODIFY_EXCEEDING: 'You cannot modify a subject with permissions you do not have',
    SELF_LOCKOUT: 'You cannot change your own role or disable yourself',
    SELF_DELETE: 'You cannot delete yourself',
    LAST_ADMIN: 'At least one enabled administrator must remain',
    BUILTIN_ROLE_IMMUTABLE: 'Built-in roles cannot be modified',
    ROLE_IN_USE: 'That role is still assigned to users',
    USER_NOT_FOUND: 'User not found',
    ROLE_NOT_FOUND: 'Role not found',
    USERNAME_TAKEN: 'That username is already taken',
    ROLE_CODE_TAKEN: 'That role code is already taken',
    INTERNAL_ERROR: 'Internal server error. Please try again later'
  },
  theme: {
    themeDrawerTitle: 'Theme Configuration',
    tabs: {
      appearance: 'Appearance',
      layout: 'Layout',
      general: 'General',
      preset: 'Preset'
    },
    appearance: {
      themeSchema: {
        title: 'Theme Schema',
        light: 'Light',
        dark: 'Dark',
        auto: 'Follow System'
      },
      grayscale: 'Grayscale',
      colourWeakness: 'Colour Weakness',
      themeColor: {
        title: 'Theme Color',
        primary: 'Primary',
        info: 'Info',
        success: 'Success',
        warning: 'Warning',
        error: 'Error',
        followPrimary: 'Follow Primary'
      },
      themeRadius: {
        title: 'Theme Radius'
      },
      recommendColor: 'Apply Recommended Color Algorithm',
      recommendColorDesc: 'The recommended color algorithm refers to',
      preset: {
        title: 'Theme Presets',
        apply: 'Apply',
        applySuccess: 'Preset applied successfully',
        default: {
          name: 'Default Preset',
          desc: 'Default theme preset with balanced settings'
        },
        dark: {
          name: 'Dark Preset',
          desc: 'Dark theme preset for night time usage'
        },
        compact: {
          name: 'Compact Preset',
          desc: 'Compact layout preset for small screens'
        },
        azir: {
          name: "Azir's Preset",
          desc: 'It is a cold and elegant preset that Azir likes'
        }
      }
    },
    layout: {
      layoutMode: {
        title: 'Layout Mode',
        vertical: 'Vertical Mode',
        horizontal: 'Horizontal Mode',
        'vertical-mix': 'Vertical Mix Mode',
        'vertical-hybrid-header-first': 'Left Hybrid Header-First',
        'top-hybrid-sidebar-first': 'Top-Hybrid Sidebar-First',
        'top-hybrid-header-first': 'Top-Hybrid Header-First',
        vertical_detail: 'Vertical menu layout, with the menu on the left and content on the right.',
        'vertical-mix_detail':
          'Vertical mix-menu layout, with the primary menu on the dark left side and the secondary menu on the lighter left side.',
        'vertical-hybrid-header-first_detail':
          'Left hybrid layout, with the primary menu at the top, the secondary menu on the dark left side, and the tertiary menu on the lighter left side.',
        horizontal_detail: 'Horizontal menu layout, with the menu at the top and content below.',
        'top-hybrid-sidebar-first_detail':
          'Top hybrid layout, with the primary menu on the left and the secondary menu at the top.',
        'top-hybrid-header-first_detail':
          'Top hybrid layout, with the primary menu at the top and the secondary menu on the left.'
      },
      tab: {
        title: 'Tab Settings',
        visible: 'Tab Visible',
        cache: 'Tag Bar Info Cache',
        cacheTip: 'Keep the tab bar information after leaving the page',
        height: 'Tab Height',
        mode: {
          title: 'Tab Mode',
          slider: 'Slider',
          chrome: 'Chrome',
          button: 'Button'
        },
        closeByMiddleClick: 'Close Tab by Middle Click',
        closeByMiddleClickTip: 'Enable closing tabs by clicking with the middle mouse button'
      },
      header: {
        title: 'Header Settings',
        height: 'Header Height',
        breadcrumb: {
          visible: 'Breadcrumb Visible',
          showIcon: 'Breadcrumb Icon Visible'
        }
      },
      sider: {
        title: 'Sider Settings',
        inverted: 'Dark Sider',
        width: 'Sider Width',
        collapsedWidth: 'Sider Collapsed Width',
        mixWidth: 'Mix Sider Width',
        mixCollapsedWidth: 'Mix Sider Collapse Width',
        mixChildMenuWidth: 'Mix Child Menu Width',
        autoSelectFirstMenu: 'Auto Select First Submenu',
        autoSelectFirstMenuTip:
          'When a first-level menu is clicked, the first submenu is automatically selected and navigated to the deepest level'
      },
      footer: {
        title: 'Footer Settings',
        visible: 'Footer Visible',
        fixed: 'Fixed Footer',
        height: 'Footer Height',
        right: 'Right Footer'
      },
      content: {
        title: 'Content Area Settings',
        scrollMode: {
          title: 'Scroll Mode',
          tip: 'The theme scroll only scrolls the main part, the outer scroll can carry the header and footer together',
          wrapper: 'Wrapper',
          content: 'Content'
        },
        page: {
          animate: 'Page Animate',
          mode: {
            title: 'Page Animate Mode',
            fade: 'Fade',
            'fade-slide': 'Slide',
            'fade-bottom': 'Fade Zoom',
            'fade-scale': 'Fade Scale',
            'zoom-fade': 'Zoom Fade',
            'zoom-out': 'Zoom Out',
            none: 'None'
          }
        },
        fixedHeaderAndTab: 'Fixed Header And Tab'
      }
    },
    general: {
      title: 'General Settings',
      watermark: {
        title: 'Watermark Settings',
        visible: 'Watermark Full Screen Visible',
        text: 'Custom Watermark Text',
        enableUserName: 'Enable User Name Watermark',
        enableTime: 'Show Current Time',
        timeFormat: 'Time Format'
      },
      multilingual: {
        title: 'Multilingual Settings',
        visible: 'Display multilingual button'
      },
      globalSearch: {
        title: 'Global Search Settings',
        visible: 'Display GlobalSearch button'
      }
    },
    configOperation: {
      copyConfig: 'Copy Config',
      copySuccessMsg: 'Copy Success, Please replace the variable "themeSettings" in "src/theme/settings.ts"',
      resetConfig: 'Reset Config',
      resetSuccessMsg: 'Reset Success'
    }
  },
  route: {
    login: 'Login',
    403: 'No Permission',
    404: 'Page Not Found',
    500: 'Server Error',
    'iframe-page': 'Iframe',
    home: 'Home',
    system: 'System',
    system_user: 'Users',
    system_role: 'Roles',
    system_permission: 'Permissions'
  },
  page: {
    login: {
      common: {
        loginOrRegister: 'Login / Register',
        userNamePlaceholder: 'Please enter user name',
        phonePlaceholder: 'Please enter phone number',
        codePlaceholder: 'Please enter verification code',
        passwordPlaceholder: 'Please enter password',
        confirmPasswordPlaceholder: 'Please enter password again',
        codeLogin: 'Verification code login',
        confirm: 'Confirm',
        back: 'Back',
        validateSuccess: 'Verification passed',
        loginSuccess: 'Login successfully',
        welcomeBack: 'Welcome back, {userName} !'
      },
      pwdLogin: {
        title: 'Password Login',
        adminTip: 'Accounts are created by an administrator. Ask one to reset your password.'
      },
      codeLogin: {
        title: 'Verification Code Login',
        getCode: 'Get verification code',
        reGetCode: 'Reacquire after {time}s',
        sendCodeSuccess: 'Verification code sent successfully',
        imageCodePlaceholder: 'Please enter image verification code'
      },
      register: {
        title: 'Register',
        agreement: 'I have read and agree to',
        protocol: '《User Agreement》',
        policy: '《Privacy Policy》'
      },
      resetPwd: {
        title: 'Reset Password'
      },
      bindWeChat: {
        title: 'Bind WeChat'
      }
    },
    home: {
      greeting: 'Hello, {userName}',
      desc: 'Manage users, roles and permissions of biw here.',
      noPermission: 'No permissions have been granted to this account. Ask an administrator.'
    },
    systemUser: {
      keywordPlaceholder: 'Search username or nickname',
      username: 'Username',
      nickname: 'Nickname',
      roles: 'Roles',
      status: 'Status',
      createdAt: 'Created at',
      createTitle: 'New user',
      editTitle: 'Edit user',
      password: 'Password',
      usernameRequired: 'Please enter a username',
      usernameInvalid: '3-32 characters starting with a letter or digit; letters, digits, "_", "." and "-" only',
      passwordRequired: 'Please enter a password',
      passwordTooShort: 'Password must be at least 8 characters',
      passwordTooLong: 'Password must be at most 64 characters',
      passwordTooManyBytes: 'Password must not exceed 72 bytes (about 24 Chinese characters)',
      resetPasswordTitle: 'Reset password',
      newPassword: 'New password',
      selfResetPasswordTip: 'This is your own account: resetting signs you out immediately.',
      enabled: 'Enabled',
      disabled: 'Disabled',
      noRole: 'No role'
    },
    systemRole: {
      keywordPlaceholder: 'Search role code or name',
      code: 'Code',
      name: 'Name',
      description: 'Description',
      status: 'Status',
      builtIn: 'Built-in',
      createdAt: 'Created at',
      createTitle: 'New role',
      editTitle: 'Edit role',
      codeTip: 'Starts with an uppercase letter; only A-Z, 0-9 and _ are allowed. Immutable afterwards.',
      codeRequired: 'Please enter a role code',
      codeInvalid: 'Must start with an uppercase letter, only A-Z, 0-9 and _ allowed, 1-32 characters',
      nameRequired: 'Please enter a role name',
      nameTooLong: 'Role name must be at most 64 characters',
      descriptionTooLong: 'Description must be at most 255 characters',
      permissionTitle: 'Assign permissions',
      permissionTip: 'The submitted set replaces the current one; an empty selection revokes everything.',
      permissionEmpty: 'No permission selected',
      permissionBuiltInTip: 'Built-in role permissions cannot be changed.',
      permissionCatalogFailed: 'Could not load the permission catalog. Try again before assigning permissions.',
      builtInRoleTip: 'Built-in roles cannot be disabled or deleted.',
      assignSuccess: 'Permissions updated',
      enabled: 'Enabled',
      disabled: 'Disabled'
    },
    systemPermission: {
      code: 'Code',
      name: 'Name',
      description: 'Description',
      readOnlyTip: 'The catalog is defined in code and seeded by sql/init.sql; it is read-only here.'
    }
  },
  form: {
    required: 'Cannot be empty',
    userName: {
      required: 'Please enter user name',
      invalid: 'User name format is incorrect'
    },
    phone: {
      required: 'Please enter phone number',
      invalid: 'Phone number format is incorrect'
    },
    pwd: {
      required: 'Please enter password',
      invalid: 'Password must be 8-64 characters and at most 72 bytes in UTF-8'
    },
    confirmPwd: {
      required: 'Please enter password again',
      invalid: 'The two passwords are inconsistent'
    },
    code: {
      required: 'Please enter verification code',
      invalid: 'Verification code format is incorrect'
    },
    email: {
      required: 'Please enter email',
      invalid: 'Email format is incorrect'
    }
  },
  dropdown: {
    closeCurrent: 'Close Current',
    closeOther: 'Close Other',
    closeLeft: 'Close Left',
    closeRight: 'Close Right',
    closeAll: 'Close All',
    pin: 'Pin Tab',
    unpin: 'Unpin Tab'
  },
  icon: {
    themeConfig: 'Theme Configuration',
    themeSchema: 'Theme Schema',
    lang: 'Switch Language',
    fullscreen: 'Fullscreen',
    fullscreenExit: 'Exit Fullscreen',
    reload: 'Reload Page',
    collapse: 'Collapse Menu',
    expand: 'Expand Menu',
    pin: 'Pin',
    unpin: 'Unpin'
  },
  datatable: {
    itemCount: 'Total {total} items',
    fixed: {
      left: 'Left Fixed',
      right: 'Right Fixed',
      unFixed: 'Unfixed'
    }
  }
};

export default local;
