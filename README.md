# biw

Java 21 + Spring Boot 4.1.2 service. PostgreSQL + Redis + MyBatis-Plus + Sa-Token + MapStruct.

## 启动

```bash
docker compose up -d
mvn spring-boot:run
```

接口文档（Swagger UI）：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON 在 `/v3/api-docs`。
只在 dev / test 打开；其他环境默认关闭，需要时设置 `SPRINGDOC_ENABLED=true`。
