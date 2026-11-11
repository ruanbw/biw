# 扩展这个模板

本文件是新增业务模块、以及从一个新项目派生本模板的唯一操作说明。规则只有一条：**机制在模板里，策略在项目里。**

---

## 1. 模板负责什么

| 能力 | 在模板里 | 在哪 |
|---|---|---|
| 表结构 | 单个 `sql/init.sql`，容器首次启动时执行 | `sql/init.sql` |
| 权限模型 | 权限码 + 内置角色 + 委派上限 | `entity/rbac/`、`service/impl/*Guards.java` |
| 认证 | Sa-Token，token 只走请求头 | `config/SaTokenSecurity.java` |
| 鉴权 | 每次请求实时查库，无缓存 | `RbacAuthorities`、`StpInterfaceImpl` |
| 审计 | `@Audited` + `AuditRecorder` | `common/audit/` |
| 错误码 | `BizCode`，与审计事件同源 | `common/error/BizCode.java` |
| 可测性 | `CurrentUser` / `Clock` / `SessionRevoker` 注入 | `common/security/` |
| 管理端 | 用户/角色/权限三个模块 | `admin/src/views/system/` |

**模板不提供**：自助改密、MFA、会话数上限、限流阈值、乐观锁、权限缓存、审计落库。这些是策略，派生项目按需实现，接入点见文末。

---

## 2. 关于表结构

**这里没有迁移链，只有 `sql/init.sql` 一个文件。** 这是刻意的：基座项目没有需要保留的历史部署，一份 schema 就够，不需要两个副本互相漂移。

改表结构的流程：

```bash
# 1. 直接编辑 sql/init.sql
# 2. 清空数据卷重建
docker compose down -v && docker compose up -d
```

当派生项目真的开始保留线上数据时，**那时**再引入迁移工具，把 `sql/init.sql` 作为第一个版本。在那之前引入只会增加维护面。

---

## 3. 新增一个业务模块（以 `order` 为例）

命名：框架表用 `sys_` 前缀，业务表**不加前缀**（`order`、`order_item`）；Java 包同名；前端 i18n key 用 `page.order.*`。

| # | 落点 | 文件 | 是否需人工同步 |
|---|---|---|---|
| 1 | 建表 + 权限种子 | `sql/init.sql`（加 `CREATE TABLE` 与 `INSERT INTO sys_permission`） | ✅ |
| 2 | 权限码常量 | `entity/rbac/PermissionCodes.java`（常量 + `ALL` 各一行） | ✅ |
| 3 | 错误码 | `common/error/BizCode.java`（一个枚举常量内含 HTTP 状态 + code + 审计事件） | ❌ 编译期强制 |
| 4 | 实体 | `entity/order/{Order,OrderStatus,dto/,vo/}` | ❌ |
| 5 | Mapper | `mapper/OrderMapper.java` | ❌ |
| 6 | 服务 | `service/OrderService.java` + `impl/OrderServiceImpl.java` | ❌ |
| 7 | 审计埋点 | 写方法上加 `@Audited(action = "order.create", targetType = "order", targetIdFromResult = true)` | ❌ |
| 8 | 控制器 | `controller/OrderController.java` + `@SaCheckPermission(PermissionCodes.ORDER_READ)` | ❌ |
| 9 | 前端 API | `admin/src/service/api/order.ts` | ❌ |
| 10 | 前端路由 | `admin/src/router/elegant/routes.ts`，`meta.permissions: ['order:read']`（**只允许一个码**） | ✅ |
| 11 | 前端 i18n | `admin/src/locales/langs/{zh-cn,en-us}.ts` + `admin/src/typings/app.d.ts` 的 Schema | ✅ |
| 12 | 前端视图 | `admin/src/views/order/index.vue`，按钮用 `hasAuth('order:write')` | ❌ |

**菜单不需要改**：Soybean 从 `routes.ts` 的 `meta.title` + i18n 生成。

### 权限码的一致性由测试保证

- `PermissionCodesTest`：`PermissionCodes.ALL` 里的每个码都必须出现在 `sql/init.sql` 里。
- `TemplateIntegrityTest`：前端 `routes.ts` 里声明的每个码都必须在 `PermissionCodes.ALL` 里。
- `AuthorizationAnnotationTest`：每个 `/api/**` handler 都必须声明权限码。

新增一个权限码，这三处会同时约束你。

### 如果新增枚举值

PostgreSQL 的 `ALTER TYPE ... ADD VALUE` 不能在同一个事务里被使用。既然是重建卷的方式，直接在 `sql/init.sql` 的 `CREATE TYPE` 里加值即可，不用考虑这一点。

---

## 4. 派生一个新项目

| # | 步骤 | 完成标准 |
|---|---|---|
| 1 | `git clone <tpl> acme && cd acme && rm -rf .git && git init` | `git log` 只有自己的提交 |
| 2 | 改名：`grep -rl 'com\.biw' src admin \| xargs sed -i '' 's/com\.biw/com\.acme/g'`，再改 `pom.xml` 的 groupId/artifactId、`spring.application.name`、Redis key 前缀 `biw:`、审计 logger 名 | `grep -rn 'com\.biw\|"biw:' .` 无输出 |
| 3 | 改 `docker-compose.yml` 的 `POSTGRES_DB` / `POSTGRES_USER` / 卷名，同步改 `sql/init.sql` 的注释 | `docker compose up -d` 后库存在 |
| 4 | 确认第一个管理员的来源（见下节） | 空库启动时按环境变量创建 |
| 5 | 启动一次 | 5 张表存在，用上一步的账号能登录 |
| 6 | 前端品牌：`VITE_APP_TITLE`、`locales/langs/*.ts`、`index.html` | 登录页无 `biw` 字样 |
| 7 | 启用 CI：`.github/workflows/ci.yml` 已就绪 | PR 上 `backend` 与 `frontend` 两个 job 绿 |
| 8 | 加一个 demo 模块（按第 3 节） | 菜单可见、CRUD 可用、`PermissionCodesTest` 绿 |

第 8 步通过才算模板可用。

---

## 5. 第一个管理员

仓库里**不保存任何账号**。一个提交过的口令就是一个公开口令：拿到仓库（或 fork、或旧压缩包）的人，
对每个按文档起起来的实例都有管理员权限。

第一个管理员由 `BootstrapAdminRunner` 从环境变量创建，只在账号表为空时执行，创建后日志 WARN
提醒改密，重启不会覆盖已有账号：

```bash
BIW_BOOTSTRAP_ADMIN_USERNAME=admin BIW_BOOTSTRAP_ADMIN_PASSWORD='<至少 8 位>' ...
```

后续管理员通过 `POST /api/users` 创建；密码只能由管理员重置（没有自助注册，也没有自助改密）。
派生项目若要接 LDAP、邀请码或 SSO，替换这个 Runner 即可，schema 不用动。

---

## 6. 策略扩展点

| 想做的事 | 接在哪 |
|---|---|
| 限流阈值 | `biw.security.login-throttle.*` |
| 会话数上限 | `application.yml` 的 `sa-token.max-login-count` |
| 可信代理 / IP 来源 | `common/web/ClientAddress` + `server.forward-headers-strategy` |
| 审计落库 | 实现 `AuditRecorder`，替换 `LoggingAuditRecorder` |
| 权限缓存 | 替换 `RbacAuthorities`（已是 Service，无静态调用） |
| 乐观锁 | 给业务表加 `version` 列 + `If-Match` 请求头 |
| 自助改密 / MFA | 新增端点，挂在 `/api/auth` 下 |
| CORS | `biw.web.cors.allowed-origins`（留空 = 不发 CORS 头） |
| 数据迁移工具 | 保留线上数据时引入，以 `sql/init.sql` 为第一个版本 |

---

## 7. 不要做的事

- 不要在 Service 里直接调 `StpUtil`。用 `CurrentUser` / `SessionIssuer` / `SessionRevoker`，否则写路径又变得不可单测。
- 不要用 `OffsetDateTime.now()`。用注入的 `Clock`，否则时间相关的断言无法复现。
- 不要在 `BizException` 里传裸字符串消息。用 `BizCode`，前端的 i18n 和测试的断言都依赖稳定的 `errorCode`。
- 不要用带 `GET` 的查询参数传 token。token 只在 `satoken` 请求头里。
- 不要在 `meta.permissions` 里写多个权限码。前端菜单是 OR 语义、后端接口是 AND 语义，写两个码会出现「能进页面但页内全 403」。
- 不要在 `sql/init.sql` 之外再维护一份 schema。
