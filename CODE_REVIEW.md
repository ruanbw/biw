# biw 代码审查报告

- **审查时间**：2026-11-12
- **范围**：`src/main/java`（78 文件 / 约 4.7k 行）、`src/test/java`（28 文件 / 约 1.7k 行）、`admin/` 里项目自己写的部分（页面、请求层、store、路由守卫、typings）、`sql/init.sql`、`application*.yml`、`pom.xml`、`docker-compose.yml`、`.github/workflows`、`docs/`
- **方法**：逐文件阅读；关键结论对照依赖源码核实（@sa/axios、pinia、naive-ui、spring-webmvc）；检查 `logs/` 实际产物；依赖版本按 OSV + GitHub Advisory 逐版本核对
- **没有做的**：本机 Docker 未启动、PostgreSQL/Redis 都不在，所以没有跑 `mvn test` 与前端构建。文中的"测试通过"是仓库里 `target/surefire-reports` 的既存结果（26 个类 / 101 用例 / 0 失败）

---

## 一句话结论

**后端质量很高**——分层清楚、并发和越权都想过了，没有发现可利用的越权、注入或提权路径。**前端是这套代码里唯一没被同等对待的部分**：几处低级问题让核心表单直接提交不了，而且后端精心设计的错误信息在前端被整体吞掉。

---

## 建议立刻修的 6 件事

| # | 位置 | 问题 |
|---|---|---|
| 1 | `admin/src/views/system/user/index.vue:434` | `NForm` 没有 `:model`，新增/编辑用户**永远校验失败**，功能不可用 |
| 2 | `admin/src/views/system/user/index.vue:480-482` | 重置密码表单同样缺 `:model`，且 `path="newPassword"` 与绑定的 `resetPassword` 不是同一个字段 |
| 3 | `admin/src/store/modules/auth/index.ts:50` | 对 setup 语法的 store 调 `$reset()`，pinia 直接抛异常 → 退出登录在半路中断 |
| 4 | `admin/src/service/request/index.ts:52-56` | 401 分支**永远不可达** → token 过期后不清理、不跳登录页，页面变成"能点、列表全空、无提示" |
| 5 | `admin/src/service/request/index.ts:72` | 后端 `message` 永远显示不出来 → 所有中文提示退化成 `Request failed with status code 400` |
| 6 | `admin/src/views/system/role/index.vue:235-271` | 权限弹窗先打开再请求：详情请求失败时弹窗不关、选择为空、"确定"仍可点 → **一次误点清空该角色全部权限** |

---

## P0 · 前端功能不可用

### 1. 新增/编辑用户表单无法提交
`admin/src/views/system/user/index.vue:434`

```vue
<NForm ref="formRef" label-placement="top" :rules="rules">
```

没有 `:model="formModel"`。naive-ui 表单项取值是 `get(NForm.props.model, path)`（`node_modules/naive-ui/es/form/src/FormItem.mjs:136`），model 为空时 `username` 的值恒为 `undefined`，`rules` 里的 `required` 必然失败 → `formRef.validate()` 恒 reject → `submitForm()` 在 `:282` 提前 return。

**修复**：`:model="formModel"`。

### 2. 重置密码表单不可用
`admin/src/views/system/user/index.vue:480`

- `<NForm ref="resetFormRef" ... :rules="resetRules">` 同样缺 `:model`；
- 规则键是 `newPassword`（`:276`），表单项 `path="newPassword"`（`:481`），但输入框绑的是另一个变量 `resetPassword`（`:482`），即使补上 model 也读不到值。

**修复**：给表单一个 `{ newPassword: resetPassword }` 形态的 model，并让 `v-model:value` 指向同一字段。

### 3. 退出登录会在中途抛异常
`admin/src/store/modules/auth/index.ts:50`

`defineStore(id, () => {...})` 是 setup 语法，`$reset()` 在 pinia 里对 setup store 直接 `throw new Error("Store ... does not implement $reset()")`（`node_modules/pinia/dist/pinia.js:1086`，已核实，不是仅 dev 行为）。

后果：`resetStore()` 在 `$reset()` 处中断，后面的 `toLogin()`、`tabStore.cacheTabs()`、`routeStore.resetStore()` 全都不执行；`logout()`（`:189-192`）收到一个 unhandled rejection。用户点退出后停原页面，内存里的 `token` 也还在。

**修复**：删掉 `$reset()`，手写重置（`token.value = ''` + `Object.assign(userInfo, 初始值)`），或给 store 暴露 `resetState()`。

### 4. 会话过期后前端完全没有反应
`admin/src/service/request/index.ts:52`

`onBackendFail` 只在 **HTTP 200 且 `code !== 0`** 时被调用（`node_modules/@sa/axios/src/index.ts:59-63`）。而 `validateStatus: isHttpSuccess`（`src/options.ts:51`）只放行 2xx/304，后端所有失败都是真实 HTTP 状态码（`GlobalExceptionHandler.java:242-244`）→ 401 走的是 error 拦截器 → `onError`，而 `onError` 又对 401 直接 return（`:66-68`）。

结果：token 失效后不清 localStorage、不跳登录，界面只剩空列表。**修复**：把 401 处理放进 `onError`，或在 `onBackendFail`/`onError` 两处共用同一个 `handleUnauthorized()`。

### 5. 后端错误信息在前端全部丢失
`admin/src/service/request/index.ts:72`

```ts
if (error.code === BACKEND_ERROR_CODE) {
  message = error.response?.data?.message || message;
}
```

`BACKEND_ERROR_CODE = 'BACKEND_ERROR'` 只在"HTTP 200 + 业务失败"时出现；真实 HTTP 4xx 的 `error.code` 是 `ERR_BAD_REQUEST`/`ERR_BAD_RESPONSE`。所以 `BizCode` 那套 message、`GlobalExceptionHandler` 拼的字段校验信息（"password: must be between 8 and 64 characters"）全都变成了 axios 的英文状态码文案。

**修复**：改成 `if (error.response?.data?.message) message = error.response.data.message;`，再按 `errorCode` 做 i18n 映射（当前 `App.Service.Response` 还没声明 `errorCode`，见 `admin/src/typings/app.d.ts:658`）。

### 6. 角色权限弹窗可能清空权限
`admin/src/views/system/role/index.vue:235-260`

```ts
permissionCodes.value = [];
permissionVisible.value = true;      // 先开弹窗
...
if (error || !detail) return;        // 失败后直接 return，弹窗还开着
```

`fetchGetRole(id)` 失败时不关闭弹窗，选择框为空、"确定"按钮可点，`submitPermissions()`（`:262`）会提交 `permissionCodes: []`，后端语义是"整体替换"→ 该角色权限被清空。

**修复**：请求成功后再打开弹窗，或失败时 `permissionVisible.value = false`；"确定"按钮加 `:disabled="!permissionTargetId || permissionLoading"`。

---

## P1 · 前端其他问题

- `admin/src/views/system/role/index.vue:177` / `:314`：角色新增/编辑完全没有前端校验（`NForm` 无 `:rules`、`submitForm` 无 `validate()`），空 code/name 直接发请求，叠加 P0-5 后用户只看到英文状态码。
- `admin/src/views/system/role/index.vue:289` / `:298-306`：`TableHeaderOperation` 会渲染"批量删除"按钮（`components/advanced/table-header-operation.vue:51-60`），但页面既没接 `@delete`，表格也没有选择列（对比 `user/index.vue:52`）→ 按钮点了没反应。
- `admin/src/store/modules/auth/index.ts:142-144`：`getUserInfo()` 失败时只清内存 `token`，没调 `clearAuthStorage()`，localStorage 里残留 token，路由守卫（`router/guard/route.ts:27`）据此仍认为已登录。
- `admin/src/views/system/user/index.vue:222`：编辑时把已有角色造成 `{ label: code, value: code }`，角色名丢失；且总是提交 `roleCodes` 数组，一旦行数据缺 `roleCodes` 就静默清空角色（后端支持 `null` = 不修改）。
- `admin/src/views/system/user/index.vue:189`：`loadRoleOptions` 忽略 error，失败时角色下拉静默为空。
- `admin/src/service/api/route.ts:4-19`：`/route/getConstantRoutes`、`/route/getUserRoutes`、`/route/isRouteExist` 后端不存在，靠 `.env` 里 `VITE_AUTH_ROUTE_MODE=static` 掩盖；切成 dynamic 会 404 并把用户踢下线。
- 权限码字符串散落在路由和 3 个页面（`router/elegant/routes.ts`、`views/system/{user,role}/index.vue:31`、`views/home/index.vue`），后端有 `PermissionCodes`，前端没有对应常量模块，改名无编译期保护。
- `admin/src/views/system/permission/index.vue:27`：`columns` 在 setup 里一次性求值 `$t(...)`，切语言表头不更新（另外两页用的是 computed）。
- `admin/src/constants/reg.ts:19` 已改成 8-64，但文案 `locales/langs/zh-cn.ts:339` 还写着"6-18位字符"，提示和校验互相打脸。
- `admin/src/router/guard/route.ts:161`：回退 `from` 时用了 `hash: to.hash`；`:159` `window.open` 缺 `noopener`；`views/_builtin/iframe-page/[url].vue:11` iframe 无 `sandbox`。
- token 存在 localStorage（`store/modules/auth/index.ts:134`）。全项目没有 `v-html`，但也没有 CSP；这是模板级取舍，改造成本高，至少记进文档。
- `admin/package.json` 没有 `test` 脚本，请求层/守卫层零测试。

---

## P1 · 后端与配置

### 1. 仓库自带一个可登录的超管种子账号
`sql/init.sql:172-181`、`README.md:20`

`init.sql` 写入 `admin` + BCrypt(cost 12) 哈希，注释说"密码见 README"，README 说"见 init.sql 注释"——**现在两边互相指，等于谁都没写**，但 `git log -p` 显示历史上某次提交里确实留过明文默认口令（已脱敏核对）。

影响：任何拿到仓库历史的人，对任何按 README 起起来的实例都有完整管理员权限；派生项目忘了切环境变量就会带着这个账号上线。

**修复**：种子移到 `sql/dev-seed.sql`（只由 compose 挂载），主 schema 不再种账号；`BIW_BOOTSTRAP_ADMIN_*` 设为 prod 启动必填；清历史或确认该口令从未用于真实环境。

### 2. dev 配置把明文密码和完整 token 写进了日志
`application-dev.yml:34`（`sa-token.is-log: true`）、`application-dev.yml:44-46`（`org.springframework.web: debug`）、`logback-spring.xml:84-85`（dev/local root DEBUG）

实测本机 `logs/biw.log`：

- `:417`、`:442`、`:521` — `Read "application/json" to [LoginRequest[username=admin, password=<明文>]]`
- `:430`、`:452` — `Writing [Result[... LoginVO[tokenName=satoken, tokenValue=<完整 token> ...]]]`

文件权限 `-rw-r--r--`（同机任何用户可读），按 `logback-spring.xml:42` 默认保留 30 天，`logs/archive/*.log.gz` 里同样有。`logs/` 已被 `.gitignore:95` 忽略，所以这是**磁盘/备份/日志采集**泄露，不是仓库泄露。

**修复**：`LoginRequest`/`LoginVO` 上加 `@ToString.Exclude`（或用 record 自定义 toString 遮蔽 `password`/`tokenValue`）；dev 关掉 `sa-token.is-log`，把 `org.springframework.web` 降到 info；日志 appender 加 `%replace` 遮蔽并 `umask 027`。

### 3. 登录限流存在检查-记录竞态
`service/impl/LoginThrottle.java:62-80`

`check()` 先 `multiGet` 读计数，`record()` 事后才 INCR，两者不在一个脚本里。并发 100 个失败请求可以同时通过 `check()`，一轮就能拿到远超 `max-account-failures: 10` 的尝试次数。账号维度的 10 次和地址维度的 50 次都只是"稳态上限"，不是硬上限。

**修复**：把"判断 + 自增"合并成一个 Lua 脚本（`if count >= limit then return 0 else INCR ... end`），在 `check` 的位置一次完成。

### 4. 会话 7 天、不限并发
`application.yml:94-99`

`timeout: 604800`（7 天绝对有效）、`is-concurrent: true` 且没有 `max-login-count`。被窃取的 token 最长 7 天无法靠"重新登录"挤掉，也没有轮换机制。

**修复**：管理后台 2–8 小时足够；设 `max-login-count: 3`；改密/改权限后吊销该账号其他会话（`SessionRevoker.revokeAll` 已经有，接上即可）。

### 5. Redis 无认证，而全部会话就在里面
`docker-compose.yml:38-42`、`application-prod.yml:20`

`redis-server --appendonly yes` 没有 `requirepass`；prod 的 `password: ${REDIS_PASSWORD:}` 缺失时静默无认证启动。端口绑 `127.0.0.1` 挡住了局域网，但同机进程 / 同 docker network 的容器 `redis-cli keys '*'` 就能导出 Sa-Token 全部会话。

**修复**：compose 用 `${REDIS_PASSWORD:?}`，prod 启动期校验非空。

### 6. 应用连库用的是 PostgreSQL 超级用户
`docker-compose.yml:15-17`、`sql/init.sql:13-14`、`application-dev.yml:5-6`

官方镜像的 `POSTGRES_USER` 是 superuser，应用侧任何一处 SQL 注入或凭据泄露 = 数据库完全控制，也没有"迁移账号/应用账号"的分层。

**修复**：`biw_ddl` 跑 init.sql，`biw_app` 只授 5 张表的 DML。

### 7. `role:write` 可以改内置角色的名字和描述
`service/impl/RoleServiceImpl.java:248-255` + `service/impl/PrivilegeGuards.java:77-87`

`checkCanModifyRole` 用"角色当前权限 ⊆ 调用者权限"判断，而 ADMIN 的权限不走 `sys_role_permission`（是代码里的 `*`），查出来是空集 → 判断直接放行。于是任何持有 `role:write` 的非管理员都能把 ADMIN 改名成"普通用户"、把描述改成别的东西。`RoleGuards.checkUpdate` 只挡了"禁用内置角色"，没挡改名。

不影响真实权限（code 没变），但足够做社工。**修复**：内置角色连 name/description 一起锁，或对 `built_in` 角色直接要求 `isAdmin()`。

### 8. 批次删除的审计丢了目标
`service/impl/UserServiceImpl.java:299`

`@Audited(action = "user.batch-delete", targetType = "user")` 没写 `targetIdParam`，默认值是 `"id"`，而参数名叫 `ids` → 审计行是 `target=user:-`，删了谁没记录。顺带：**登录成功、登出都没有审计**（只有失败有），成功登录恰恰是安全审计最该有的一条。

### 9. 审计只有日志文件一条出口
`common/audit/LoggingAuditRecorder.java` + `logback-spring.xml:42-43`

`maxHistory` 30 天、`totalSizeCap` 5GB，应用进程自己就能改写。`docs/EXTENDING.md` 已经写明这是扩展点，但"基座"至少该给一张 `sys_audit_log` 表和独立 appender，否则出事时拿不出证据。

---

## P2 · 后端正确性/健壮性

- `service/impl/UserServiceImpl.java:129-137`：`me()` 不校验 `status`。禁用账号若会话没被成功吊销（Redis 抖动，或直接改库禁用），`/api/auth/me` 仍返回 200 + 档案；`/api/auth/permissions` 返回空。建议 `status != ENABLED` 直接抛 `SESSION_INVALID`。
- `common/web/RequestIdFilter.java:35-38`：入站 `X-Request-Id` 只判了长度 ≤128，没限字符集，原样进 MDC、日志和响应头。带换行的值可以伪造日志行（`LoggingAuditRecorder.format` 只对 `detail` 做了换行清洗，`requestId` 这条路径没洗）。建议 `^[A-Za-z0-9._:-]{1,128}$`，不合规就换新 UUID。
- `controller/ApiErrorController.java:31-32`：容器级错误用 `Result.fail(code, reasonPhrase)`，`errorCode` 是 null，和 `GlobalExceptionHandler` 的信封不一致；前端按 `errorCode` 做本地化时会踩空。
- `service/impl/UserServiceImpl.java:250-272`：`update()` 即使用户什么都没改（三个字段全 null）也会写一次 `updated_at` 并返回 200；语义上是"空更新成功"，`recordChanges` 也就写不出任何变化。建议全 null 直接返回当前对象。
- `bootstrap/BootstrapAdminRunner.java:95`：报错文案说"the V2 migration did not run"，但这个项目**没有迁移链**（`sql/init.sql` 自己写明了）。文案会误导排障。
- `entity/rbac/PermissionCodes.java:8`：注释同样提到 "the migrations"，实际是 `sql/init.sql`。
- `entity/user/dto/LoginRequest.java:14`、`entity/user/dto/CreateUserRequest.java:21`：`@Schema(example = ...)` 里是**看起来像真实口令**的字符串。dev/test 的 Swagger UI 打开时会预填；万一和种子口令一致，等于在 Java 源码里又复制了一份凭据。建议换成 `P@ssw0rd-example` 这类明显假值。
- `application-prod.yml:36` + `common/web/ClientAddress.java:27`：`forward-headers-strategy: native` 会无条件信任入站 `X-Forwarded-For`。如果 8080 端口除了反代还能被直连，攻击者每次换一个伪造 XFF 就能绕过按 IP 的 50 次上限（账号维度仍有效）。建议配 `server.tomcat.remoteip.internal-proxies` 或改 `framework` 策略。
- `sql/init.sql`：`updated_at` 没有触发器，完全靠应用写（`AdminGuardLock.java:8` 的注释自己都承认这是 "an accident, not a guarantee"）；`sys_role.name` 没有唯一约束，可以存在两个同名角色；`password_hash` 没有格式 CHECK。

---

## P2 · 测试覆盖

结构上有两个缺口：

1. **服务层写路径没有测试**。`UserServiceImpl` / `RoleServiceImpl` 里最要命的编排——"先拿 advisory lock 再数管理员"、"先 resolve 角色再判 last-admin"、"哪个 guard 用什么参数调用"——只有 `UserGuardsTest` / `RoleGuardsTest` / `PrivilegeGuardsTest` 这些纯决策单测覆盖，组合起来的行为没有任何回归保护。`UserServiceImplLoginTest` / `UserServiceImplOptionsTest` 只测了登录和下拉。
2. **没有集成测试层**。`ci.yml:53-66` 把 `backend-it` 注释掉了（需要 Testcontainers + Docker）。所以"两个并发请求同时移除最后一个管理员"这种核心不变量，实际只验证了锁存在，没验证锁有效。

`AuthorizationAnnotationTest` 是这套测试里最值钱的一个：它遍历所有 `/api/**` handler，要求要么声明权限码、要么在 `AUTH_ONLY` 白名单里，新增接口漏注解会直接失败。这类测试应该再多几个。

---

## 配置/依赖/供应链

- **依赖版本很干净**：Spring Boot 4.1.2、Sa-Token 1.46.0、MyBatis-Plus 3.5.18.1、pgjdbc 42.7.14 都是各自线上的最新稳定版；按 OSV + GitHub Advisory 逐 `group:artifact@version` 核对，**当前版本零 CVE 命中**（对照组 log4j 2.14.1 / pgjdbc 42.7.1 能正常命中，说明查询链路有效）。没有引入 jjwt，认证全走 Sa-Token + Redis 会话，攻击面比手搓 JWT 小。
- 真正缺的是**机制**而不是升级：`.github/workflows/ci.yml` 没有 `permissions:` 块，action 全用可移动的 major tag（`@v4`）；没有 Dependabot/Renovate、没有 SBOM、没有 dependency-check；`pnpm install --frozen-lockfile` 没加 `--ignore-scripts`，且 `admin/.npmrc` 指向 npmmirror 镜像。
- `application.yml` 没有显式收敛 `management.*`（靠 Boot 默认只开 health），`server.error.include-*` 只在 prod 收敛——建议上移到 `application.yml`，让新 profile 默认继承安全的那一侧。
- `.gitignore:74-82` 用反向白名单把 `admin/.env`、`.env.test`、`.env.prod` 捞出来了。我核对过这三个文件，当前只有 `VITE_*` 构建变量、**没有密钥**；但给"以后往这里塞真密钥"留了门，建议只保留 `.env.example`。同时缺 `*.pem`、`*.key`、`*.p12`、`*.jks`。
- `docker-compose.yml` 镜像浮动 tag（`postgres:18-alpine`、`redis:8-alpine`），构建不可复现；缺 `cap_drop`/`read_only`/`user:`/日志大小限制。
- 双 Jackson 并存（Boot 4 的 `tools.jackson:3.1.7` + springdoc 拉进来的 `com.fasterxml:2.21.7`），功能上没问题，但要盯两条 CVE 线。

---

## 我特意验证过、**不是**问题的部分

写下来是为了避免下一轮重复怀疑：

- **CORS 预检不会被 Sa-Token 挡**。`SaTokenConfigure` 的拦截器挂在 `/api/**`，看起来 OPTIONS 会被 `checkLogin()` 拦成 401。我去翻了 spring-webmvc 7.0.10 的 `AbstractHandlerMapping#getCorsHandlerExecutionChain`（`:746-757`）：预检请求会被替换成 `PreFlightHttpRequestHandler` 并**插到拦截器链第 0 位**且返回 false，Sa-Token 的拦截器根本不会执行。跨域部署是安全的。
- **没有 SQL 注入**。手写 SQL 全部 `#{}`；`.apply("username ILIKE {0} ...")` 是参数占位符；`LIKE` 的 `%`/`_`/`\` 在 `OptionQuery.pattern` 里做了转义。
- **没有发现越权提权路径**。逐个走了 create/update/delete/resetPassword/assignPermissions：管理员保护、自我锁死、最后一个管理员、授予上限、目标权限上限都覆盖到了，批量删除还专门把"禁用的管理员"也算进去。
- **并发不变量**：`pg_advisory_xact_lock` + 角色行 `FOR SHARE`/`FOR UPDATE` 的获取顺序没有环路，`update` 里"先 resolve 再判 last-admin"的先后顺序也是对的（未知角色码返回 400 而不是误导性的 403）。
- **CSRF 面很小**：token 只走 `satoken` 头，`is-read-cookie: false`，`allowCredentials(false)`。
- **密码处理**：BCrypt cost 12、未知用户也跑一次 BCrypt（时间侧信道）、72 字节上限、登录失败同时计账号桶和地址桶且账号名哈希后入 Redis——这部分是教科书级。

---

## 建议的修复顺序

1. 前端 6 个 P0（半天，用户立刻能正常用）
2. 日志脱敏 + 种子账号改成 dev-only（半天，风险最高的一类）
3. 登录限流改 Lua 原子操作、会话时长/并发收敛（半天）
4. Redis 密码、DB 最小权限、CI `permissions:` + Dependabot（一小时）
5. 服务层写路径补测试（1–2 天，为后续改动兜底）
