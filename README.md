# biw

Java 21 + Spring Boot 4.1.2 service. PostgreSQL + Redis + MyBatis-Plus + Sa-Token + MapStruct.

## 启动

```bash
docker compose up -d
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

profile 没有默认值：不设置 `SPRING_PROFILES_ACTIVE` 会启动失败，而不是悄悄以 dev 配置跑起来。
`dev` / `test` / `prod` 之外还有 `local`（个人机器覆盖，`application-local.yml` 不入库）。

接口文档（Swagger UI）：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON 在 `/v3/api-docs`。
只在 dev / test 打开；其他环境默认关闭，需要时设置 `SPRINGDOC_ENABLED=true`。

## 管理端

`admin/` 是配套的后台管理前端（Vue 3 + TypeScript + Naive UI，基于 Soybean Admin），
覆盖用户、角色、权限三个模块：

```bash
cd admin
pnpm install
pnpm dev        # http://localhost:9527
```

开发期通过 Vite 代理转发到 `http://localhost:8080`，不需要给后端加 CORS 配置。
后端换了端口就改 `admin/.env.test` 里的 `VITE_SERVICE_BASE_URL`。

没有公开注册：账号由管理员创建，密码也只能由管理员重置。第一个管理员按 `sql/init.sql`
末尾的注释手动插入。
