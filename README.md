# biw

Java 21 + Spring Boot 4.1.2 的 **RBAC 基座项目**：用户、角色、权限、登录，配套 Vue 3 管理端。
它不是一个完整产品，而是一个可直接开工的骨架——新增业务模块和派生新项目的步骤见
[docs/EXTENDING.md](docs/EXTENDING.md)。

技术栈：PostgreSQL 18 + Redis + MyBatis-Plus + Sa-Token + MapStruct，
管理端是 Vue 3 + TypeScript + Naive UI（基于 Soybean Admin）。

## 启动

```bash
docker compose up -d                # 建库 + 建表 + 建应用账号（见下）
```

第一个管理员从环境变量创建，仓库里不保存任何账号或口令：

```bash
BIW_BOOTSTRAP_ADMIN_USERNAME=admin \
BIW_BOOTSTRAP_ADMIN_PASSWORD='<至少 8 位，UTF-8 不超过 72 字节>' \
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

`BootstrapAdminRunner` 只在账号表为空时创建，账号已存在时这两个变量被忽略，所以可以一直带着它们启动。
**首次登录后请改密。**

数据库有两个角色：`biw`（镜像创建的超级用户，只用于执行 init 脚本）和 `biw_app`（应用真正连的
角色，只有 public schema 的 DML 权限，由 `sql/02-app-role.sh` 创建）。应用配置里的
`DB_USERNAME` / `DB_PASSWORD` 默认就是 `biw_app` / `apppass`。**升级到这套配置前已经存在的
postgres volume 里没有这个角色**，需要 `docker compose down -v && docker compose up -d` 重建
（数据会丢，本项目的 schema 本来就是"改文件 + 重建"）。

**审计表是证据，不是日志。** `biw_app` 对 `sys_audit_log` 只有 `SELECT` / `INSERT`：写进去的记录，
应用自己（以及任何借应用执行的 SQL 注入）改不了也删不了。代价是保留期清理不能交给应用，
由运维夜间执行一次：

```bash
docker compose exec -T postgres \
  psql -U biw -d biw -v retention_days=365 -f - < sql/03-audit-retention.sql
```

**表结构只有一个文件：`sql/init.sql`。** 没有迁移链——这是基座项目，没有需要保留的部署历史。
改表就改这个文件，然后 `docker compose down -v && docker compose up -d`。

`SPRING_PROFILES_ACTIVE` 没有默认值：不设置会启动失败，而不是悄悄用 dev 配置跑起来。
`dev` / `test` / `prod` 之外还有 `local`（个人机器覆盖，`application-local.yml` 不入库）。

接口文档（Swagger UI）：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON 在 `/v3/api-docs`，
只在 dev / test 打开。健康检查在 `/actuator/health`。

## 管理端

`admin/` 是配套的后台管理前端，覆盖用户、角色、权限三个模块：

```bash
cd admin
pnpm install
pnpm dev        # http://localhost:9527
```

开发期通过 Vite 代理转发到 `http://localhost:8080`，不需要给后端加 CORS 配置。
后端换了端口就改 `admin/.env.test` 里的 `VITE_SERVICE_BASE_URL`。

创建/重置密码的表单在提交前就校验长度与字节数，与后端规则一致，非法输入不会发出请求。
登录表单只校验非空——登录不应该套用"设置密码"的策略，否则一个合法但很长的口令会被前端挡住。

## 测试

```bash
mvn test                 # 单元测试 + 权限目录/前端路由的一致性检查
cd admin && pnpm typecheck && pnpm build
```

CI 定义在 `.github/workflows/ci.yml`：后端单测与前端 lint/typecheck/build 是阻断项。

## 约定

- 错误响应统一为 `Result{code, errorCode, message, data}`。**断言用 `errorCode`，不要用 `message`**——后者可能会被改写或翻译。
- 权限码、错误码、审计事件名在 `PermissionCodes` / `BizCode` 中定义，`sql/init.sql` 与前端路由由测试强制对齐。
- Service 不直接调用 `StpUtil` 或 `OffsetDateTime.now()`，改用注入的 `CurrentUser` / `Clock` / `SessionRevoker`。
- 管理端的写操作由 `@Audited` 记录审计（谁、对谁、改了什么），失败请求由错误码表统一记录。

## 没有公开注册

账号由管理员创建，密码也只能由管理员重置。没有自助注册，也没有自助改密——后者属于派生项目的业务范围。
