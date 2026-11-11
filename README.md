# biw

Java 21 + Spring Boot 4.1.2 的 **RBAC 基座项目**：用户、角色、权限、登录，配套 Vue 3 管理端。
它不是一个完整产品，而是一个可直接开工的骨架——新增业务模块和派生新项目的步骤见
[docs/EXTENDING.md](docs/EXTENDING.md)。

技术栈：PostgreSQL 18 + Redis + MyBatis-Plus + Sa-Token + MapStruct，
管理端是 Vue 3 + TypeScript + Naive UI（基于 Soybean Admin）。

## 启动

```bash
docker compose up -d                # 建库 + 执行 sql/init.sql（表、内置角色、权限目录、admin）
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

**表结构只有一个文件：`sql/init.sql`。** 没有迁移链——这是基座项目，没有需要保留的部署历史。
改表就改这个文件，然后 `docker compose down -v && docker compose up -d`。

默认管理员：用户名 `admin`，密码见 `sql/init.sql` 末尾注释。**首次登录后请改密。**

不想在仓库里保留凭据的部署，删掉 `sql/init.sql` 里的 admin 两段，改用环境变量：

```bash
BIW_BOOTSTRAP_ADMIN_USERNAME=admin \
BIW_BOOTSTRAP_ADMIN_PASSWORD='<至少 8 位，UTF-8 不超过 72 字节>' \
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

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

前端表单在提交前就校验用户名、密码长度与字节数，与后端规则一致，非法输入不会发出请求。

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
