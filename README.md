# biw

Java 21 + Spring Boot 4.1.2 service. PostgreSQL + Redis + MyBatis-Plus + Sa-Token + MapStruct + Flyway.

## 启动

```bash
docker compose up -d     # PostgreSQL + Redis
mvn spring-boot:run      # 首次启动会由 Flyway 建表
```

表结构由 Flyway 管理，脚本在 `src/main/resources/db/migration`，启动时自动执行。
数据库里已有旧表但没有迁移历史（早期用 `sql/init.sql` 建的库）时，启动会直接报错：
开发环境 `docker compose down -v` 重建即可，有数据的环境需要手工补上差异。

接口文档（Swagger UI）：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON 在 `/v3/api-docs`。
只在 dev / test 打开；其他环境默认关闭，需要时设置 `SPRINGDOC_ENABLED=true`。
