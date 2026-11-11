const local: App.I18n.Schema = {
  system: {
    title: 'biw 管理后台',
    updateTitle: '系统版本更新通知',
    updateContent: '检测到系统有新版本发布，是否立即刷新页面？',
    updateConfirm: '立即刷新',
    updateCancel: '稍后再说'
  },
  common: {
    action: '操作',
    add: '新增',
    addSuccess: '添加成功',
    backToHome: '返回首页',
    batchDelete: '批量删除',
    cancel: '取消',
    close: '关闭',
    check: '勾选',
    selectAll: '全选',
    expandColumn: '展开列',
    columnSetting: '列设置',
    config: '配置',
    confirm: '确认',
    delete: '删除',
    deleteSuccess: '删除成功',
    confirmDelete: '确认删除吗？',
    edit: '编辑',
    warning: '警告',
    error: '错误',
    index: '序号',
    keywordSearch: '请输入关键词搜索',
    logout: '退出登录',
    logoutConfirm: '确认退出登录吗？',
    lookForward: '敬请期待',
    modify: '修改',
    modifySuccess: '修改成功',
    noData: '无数据',
    operate: '操作',
    pleaseCheckValue: '请检查输入的值是否合法',
    refresh: '刷新',
    reset: '重置',
    search: '搜索',
    switch: '切换',
    tip: '提示',
    trigger: '触发',
    update: '更新',
    updateSuccess: '更新成功',
    userCenter: '个人中心',
    yesOrNo: {
      yes: '是',
      no: '否'
    }
  },
  request: {
    logout: '请求失败后登出用户',
    logoutMsg: '用户状态失效，请重新登录',
    logoutWithModal: '请求失败后弹出模态框再登出用户',
    logoutWithModalMsg: '用户状态失效，请重新登录',
    refreshToken: '请求的token已过期，刷新token',
    tokenExpired: 'token已过期'
  },
  /**
   * 一条对应后端 `BizCode` 的一个 `errorCode`。
   *
   * 请求层优先显示这里的文案，本地化缺失时才退回后端 message，所以后端改文案不需要前端跟着发版；
   * 反过来，新增 errorCode 时必须在这里和 en-us.ts 同时补上。
   */
  error: {
    VALIDATION_FAILED: '提交的内容未通过校验，请检查后重试',
    MALFORMED_BODY: '请求内容格式错误，无法解析',
    UNSUPPORTED_MEDIA_TYPE: '不支持的请求内容类型',
    MISSING_PARAMETER: '缺少必要的请求参数',
    INVALID_PARAMETER: '请求参数格式不正确',
    PAGE_TOO_DEEP: '页码过深，请缩小搜索范围后重试',
    IDS_OUT_OF_RANGE: '一次最多操作 100 项，请分批提交',
    USERNAME_REQUIRED: '用户名不能为空',
    PASSWORD_TOO_LONG: '密码不能超过 72 字节',
    ROLE_NAME_REQUIRED: '角色名称不能为空',
    UNKNOWN_ROLE: '角色不存在或已被删除',
    UNKNOWN_PERMISSION: '权限不存在或已被删除',
    ENDPOINT_NOT_FOUND: '接口不存在',
    METHOD_NOT_ALLOWED: '不支持该请求方法',
    NOT_ACCEPTABLE: '无法返回可接受的响应格式',
    BAD_CREDENTIALS: '用户名或密码不正确',
    SESSION_INVALID: '登录状态已失效，请重新登录',
    MISSING_CREDENTIAL: '缺少登录凭证，请重新登录',
    INVALID_CREDENTIAL: '登录凭证无效，请重新登录',
    SESSION_EXPIRED: '登录已过期，请重新登录',
    SESSION_REPLACED: '账号已在其他地方登录，请重新登录',
    SESSION_KICKED: '账号已被强制下线，请重新登录',
    LOGIN_THROTTLED: '登录失败次数过多，请稍后再试',
    PERMISSION_DENIED: '缺少该操作所需的权限',
    ROLE_DENIED: '缺少该操作所需的角色',
    ADMIN_REQUIRED: '仅管理员可以执行该操作',
    CANNOT_GRANT_EXCEEDING: '不能授予自己未拥有的权限',
    CANNOT_MODIFY_EXCEEDING: '不能修改权限高于自己的用户或角色',
    SELF_LOCKOUT: '不能修改自己的角色或禁用自己',
    SELF_DELETE: '不能删除自己',
    LAST_ADMIN: '至少需要保留一个启用状态的管理员',
    BUILTIN_ROLE_IMMUTABLE: '内置角色不可修改',
    ROLE_IN_USE: '该角色仍有用户在用，无法删除',
    USER_NOT_FOUND: '用户不存在',
    ROLE_NOT_FOUND: '角色不存在',
    USERNAME_TAKEN: '用户名已存在',
    ROLE_CODE_TAKEN: '角色编码已存在',
    INTERNAL_ERROR: '服务器内部错误，请稍后重试'
  },
  theme: {
    themeDrawerTitle: '主题配置',
    tabs: {
      appearance: '外观',
      layout: '布局',
      general: '通用',
      preset: '预设'
    },
    appearance: {
      themeSchema: {
        title: '主题模式',
        light: '亮色模式',
        dark: '暗黑模式',
        auto: '跟随系统'
      },
      grayscale: '灰色模式',
      colourWeakness: '色弱模式',
      themeColor: {
        title: '主题颜色',
        primary: '主色',
        info: '信息色',
        success: '成功色',
        warning: '警告色',
        error: '错误色',
        followPrimary: '跟随主色'
      },
      themeRadius: {
        title: '主题圆角'
      },
      recommendColor: '应用推荐算法的颜色',
      recommendColorDesc: '推荐颜色的算法参照',
      preset: {
        title: '主题预设',
        apply: '应用',
        applySuccess: '预设应用成功',
        default: {
          name: '默认预设',
          desc: 'Soybean 默认主题预设'
        },
        dark: {
          name: '暗色预设',
          desc: '适用于夜间使用的暗色主题预设'
        },
        compact: {
          name: '紧凑型',
          desc: '适用于小屏幕的紧凑布局预设'
        },
        azir: {
          name: 'Azir的预设',
          desc: '是 Azir 比较喜欢的莫兰迪色系冷淡风'
        }
      }
    },
    layout: {
      layoutMode: {
        title: '布局模式',
        vertical: '左侧菜单模式',
        'vertical-mix': '左侧菜单混合模式',
        'vertical-hybrid-header-first': '左侧混合-顶部优先',
        horizontal: '顶部菜单模式',
        'top-hybrid-sidebar-first': '顶部混合-侧边优先',
        'top-hybrid-header-first': '顶部混合-顶部优先',
        vertical_detail: '左侧菜单布局，菜单在左，内容在右。',
        'vertical-mix_detail': '左侧双菜单布局，一级菜单在左侧深色区域，二级菜单在左侧浅色区域。',
        'vertical-hybrid-header-first_detail':
          '左侧混合布局，一级菜单在顶部，二级菜单在左侧深色区域，三级菜单在左侧浅色区域。',
        horizontal_detail: '顶部菜单布局，菜单在顶部，内容在下方。',
        'top-hybrid-sidebar-first_detail': '顶部混合布局，一级菜单在左侧，二级菜单在顶部。',
        'top-hybrid-header-first_detail': '顶部混合布局，一级菜单在顶部，二级菜单在左侧。'
      },
      tab: {
        title: '标签栏设置',
        visible: '显示标签栏',
        cache: '标签栏信息缓存',
        cacheTip: '离开页面后仍然保留标签栏信息',
        height: '标签栏高度',
        mode: {
          title: '标签栏风格',
          slider: '滑块风格',
          chrome: '谷歌风格',
          button: '按钮风格'
        },
        closeByMiddleClick: '鼠标中键关闭标签页',
        closeByMiddleClickTip: '启用后可以使用鼠标中键点击标签页进行关闭'
      },
      header: {
        title: '头部设置',
        height: '头部高度',
        breadcrumb: {
          visible: '显示面包屑',
          showIcon: '显示面包屑图标'
        }
      },
      sider: {
        title: '侧边栏设置',
        inverted: '深色侧边栏',
        width: '侧边栏宽度',
        collapsedWidth: '侧边栏折叠宽度',
        mixWidth: '混合布局侧边栏宽度',
        mixCollapsedWidth: '混合布局侧边栏折叠宽度',
        mixChildMenuWidth: '混合布局子菜单宽度',
        autoSelectFirstMenu: '自动选择第一个子菜单',
        autoSelectFirstMenuTip: '点击一级菜单时，自动选择并导航到第一个子菜单的最深层级'
      },
      footer: {
        title: '底部设置',
        visible: '显示底部',
        fixed: '固定底部',
        height: '底部高度',
        right: '底部居右'
      },
      content: {
        title: '内容区域设置',
        scrollMode: {
          title: '滚动模式',
          tip: '主题滚动仅 main 部分滚动，外层滚动可携带头部底部一起滚动',
          wrapper: '外层滚动',
          content: '主体滚动'
        },
        page: {
          animate: '页面切换动画',
          mode: {
            title: '页面切换动画类型',
            'fade-slide': '滑动',
            fade: '淡入淡出',
            'fade-bottom': '底部消退',
            'fade-scale': '缩放消退',
            'zoom-fade': '渐变',
            'zoom-out': '闪现',
            none: '无'
          }
        },
        fixedHeaderAndTab: '固定头部和标签栏'
      }
    },
    general: {
      title: '通用设置',
      watermark: {
        title: '水印设置',
        visible: '显示全屏水印',
        text: '自定义水印文本',
        enableUserName: '启用用户名水印',
        enableTime: '显示当前时间',
        timeFormat: '时间格式'
      },
      multilingual: {
        title: '多语言设置',
        visible: '显示多语言按钮'
      },
      globalSearch: {
        title: '全局搜索设置',
        visible: '显示全局搜索按钮'
      }
    },
    configOperation: {
      copyConfig: '复制配置',
      copySuccessMsg: '复制成功，请替换 src/theme/settings.ts 中的变量 themeSettings',
      resetConfig: '重置配置',
      resetSuccessMsg: '重置成功'
    }
  },
  route: {
    login: '登录',
    403: '无权限',
    404: '页面不存在',
    500: '服务器错误',
    'iframe-page': '外链页面',
    home: '首页',
    system: '系统管理',
    system_user: '用户管理',
    system_role: '角色管理',
    system_permission: '权限目录'
  },
  page: {
    login: {
      common: {
        loginOrRegister: '登录 / 注册',
        userNamePlaceholder: '请输入用户名',
        phonePlaceholder: '请输入手机号',
        codePlaceholder: '请输入验证码',
        passwordPlaceholder: '请输入密码',
        confirmPasswordPlaceholder: '请再次输入密码',
        codeLogin: '验证码登录',
        confirm: '确定',
        back: '返回',
        validateSuccess: '验证成功',
        loginSuccess: '登录成功',
        welcomeBack: '欢迎回来，{userName} ！'
      },
      pwdLogin: {
        title: '密码登录',
        adminTip: '账号由管理员创建，忘记密码请联系管理员重置。'
      },
      codeLogin: {
        title: '验证码登录',
        getCode: '获取验证码',
        reGetCode: '{time}秒后重新获取',
        sendCodeSuccess: '验证码发送成功',
        imageCodePlaceholder: '请输入图片验证码'
      },
      register: {
        title: '注册账号',
        agreement: '我已经仔细阅读并接受',
        protocol: '《用户协议》',
        policy: '《隐私权政策》'
      },
      resetPwd: {
        title: '重置密码'
      },
      bindWeChat: {
        title: '绑定微信'
      }
    },
    home: {
      greeting: '你好，{userName}',
      desc: '这里是 biw 的用户、角色与权限管理后台。',
      noPermission: '当前账号未分配任何权限，请联系管理员'
    },
    systemUser: {
      keywordPlaceholder: '搜索用户名或昵称',
      username: '用户名',
      nickname: '昵称',
      roles: '角色',
      status: '状态',
      createdAt: '创建时间',
      createTitle: '新增用户',
      editTitle: '编辑用户',
      password: '密码',
      usernameRequired: '请输入用户名',
      usernameInvalid: '3-32 个字符，以字母或数字开头，只能包含字母、数字、下划线、点和短横线',
      passwordRequired: '请输入密码',
      passwordTooShort: '密码至少 8 个字符',
      passwordTooLong: '密码最多 64 个字符',
      passwordTooManyBytes: '密码不能超过 72 字节（中文每个约 3 字节）',
      resetPasswordTitle: '重置密码',
      newPassword: '新密码',
      selfResetPasswordTip: '这是你自己的账号：重置后当前会话会立即失效，你将退出登录。',
      enabled: '启用',
      disabled: '禁用',
      noRole: '无角色'
    },
    systemRole: {
      keywordPlaceholder: '搜索角色编码或名称',
      code: '角色编码',
      name: '角色名称',
      description: '说明',
      status: '状态',
      builtIn: '内置',
      createdAt: '创建时间',
      createTitle: '新增角色',
      editTitle: '编辑角色',
      codeTip: '大写字母开头，仅可包含大写字母、数字和下划线，创建后不可修改。',
      codeRequired: '请输入角色编码',
      codeInvalid: '须以大写字母开头，仅可包含大写字母、数字和下划线，长度为 1-32 位',
      nameRequired: '请输入角色名称',
      nameTooLong: '角色名称不能超过 64 个字符',
      descriptionTooLong: '说明不能超过 255 个字符',
      permissionTitle: '分配权限',
      permissionTip: '提交的权限会整体替换角色原有权限，留空表示撤销全部权限。',
      permissionEmpty: '未选择任何权限',
      permissionBuiltInTip: '内置角色的权限不可修改。',
      permissionCatalogFailed: '权限目录加载失败，请重试后再分配权限。',
      builtInRoleTip: '内置角色不能禁用或删除。',
      assignSuccess: '权限已更新',
      enabled: '启用',
      disabled: '禁用'
    },
    systemPermission: {
      code: '权限编码',
      name: '权限名称',
      description: '说明',
      readOnlyTip: '权限目录由后端代码定义、由 sql/init.sql 写入，这里只能查看。'
    }
  },
  form: {
    required: '不能为空',
    userName: {
      required: '请输入用户名',
      invalid: '用户名格式不正确'
    },
    phone: {
      required: '请输入手机号',
      invalid: '手机号格式不正确'
    },
    pwd: {
      required: '请输入密码',
      invalid: '密码需为 8-64 位字符，且 UTF-8 编码不超过 72 字节'
    },
    confirmPwd: {
      required: '请输入确认密码',
      invalid: '两次输入密码不一致'
    },
    code: {
      required: '请输入验证码',
      invalid: '验证码格式不正确'
    },
    email: {
      required: '请输入邮箱',
      invalid: '邮箱格式不正确'
    }
  },
  dropdown: {
    closeCurrent: '关闭',
    closeOther: '关闭其它',
    closeLeft: '关闭左侧',
    closeRight: '关闭右侧',
    closeAll: '关闭所有',
    pin: '固定标签',
    unpin: '取消固定'
  },
  icon: {
    themeConfig: '主题配置',
    themeSchema: '主题模式',
    lang: '切换语言',
    fullscreen: '全屏',
    fullscreenExit: '退出全屏',
    reload: '刷新页面',
    collapse: '折叠菜单',
    expand: '展开菜单',
    pin: '固定',
    unpin: '取消固定'
  },
  datatable: {
    itemCount: '共 {total} 条',
    fixed: {
      left: '左固定',
      right: '右固定',
      unFixed: '取消固定'
    }
  }
};

export default local;
