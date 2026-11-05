# biw

Spring Boot 服务端工程。Java 21 + Spring Boot 4.1.2。

## 技术栈

| 组件 | 版本 | 备注 |
| --- | --- | --- |
| Java | 21 | `maven.compiler.release=21` |
| Spring Boot | 4.1.2 | Spring Framework 7.0.x |
| Web | `spring-boot-starter-webmvc` | Boot 4 起 `-web` 更名为 `-webmvc` |
| 持久层 | MyBatis-Plus 3.5.18.1 | `mybatis-plus-spring-boot4-starter`，分页依赖 `mybatis-plus-jsqlparser` |
| 数据库 | PostgreSQL | 驱动版本由 Boot BOM 管理；HikariCP 连接池 |
| 缓存 / 会话 | Redis | Spring Data Redis（Lettuce）+ `commons-pool2` |
| 认证鉴权 | Sa-Token 1.46.0 | `sa-token-spring-boot4-starter` + `sa-token-redis-template`（会话落 Redis） |
| 参数校验 | Spring Boot Validation | Hibernate Validator 9 |
| 日志 | Logback | `logback-spring.xml`，多环境 |
| 测试 | JUnit 5 + MockMvc | `mvn test` |

## 目录

```
src/main/java/com/biw
├── BiwApplication.java
├── common/api/Result.java                     # 统一响应体 {code,message,data}
├── common/exception/                          # BizException / ErrorCode / 全局异常处理
├── config/SaTokenConfigure.java               # 登录拦截器与白名单
├── config/MybatisPlusConfig.java              # 分页插件
├── controller/AuthController.java             # 登录 / 登出 / 当前用户
└── controller/dto/LoginRequest.java           # 校验注解示例
src/main/java/com/biw/mapper                   # @MapperScan 扫描，放 Mapper 接口

src/main/resources
├── application.yml                            # 公共配置
├── application-{dev,test,prod}.yml            # 多环境
├── application-local.yml.example              # 本地覆盖模板（复制成 application-local.yml）
└── logback-spring.xml

sql/init.sql                                   # 首次建库时执行（扩展）
docker-compose.yml                             # 本地 PostgreSQL 18 + Redis 8
```

## 本地启动

```bash
# 1. 起 PostgreSQL 与 Redis（需要 Docker）
docker compose up -d
docker compose ps          # 等两个服务都 healthy

# 2. 启动应用（默认 dev）
mvn spring-boot:run

# 只编译打包
mvn clean package
```

默认端口 `8080`，可用 `SERVER_PORT` 覆盖。

### 接口自测

```bash
# 登录，拿到 token（demo 账号：admin / biw.auth.demo-password）
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME"}'

# 带 token 访问需要登录的接口
curl -s http://localhost:8080/api/auth/me -H 'satoken: <tokenValue>'
```

`/api/auth/login` 里的账号是占位实现（`biw.auth.demo-username` / `biw.auth.demo-password`），
接真实用户表后把 `AuthController#login` 换成查库即可。

## 多环境

通过 `SPRING_PROFILES_ACTIVE` 或 `--spring.profiles.active=` 切换，默认 `dev`。

| Profile | 数据库 | Redis | 日志 |
| --- | --- | --- | --- |
| `dev` | `localhost:5432/biw`（biw/devpass） | `localhost:6379` db0 | 控制台彩色 + 文件，DEBUG，打印 SQL |
| `test` | `biw_test`（可用 `DB_*` 覆盖） | db1 | 仅控制台 |
| `prod` | `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 控制台 + 异步文件，INFO |
| `local` | 由 `application-local.yml` 决定 | 同左 | 同 dev |

生产环境不落任何默认密码；缺少必需环境变量时启动直接失败，避免带病上线。
本地想改连接串，复制模板即可（该文件已被 git 忽略）：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

## 日志

- `logs/biw.log`：全量，按天 + 100MB 滚动，gzip 归档，保留 30 天，总量上限 5GB
- `logs/biw-error.log`：WARN 及以上，单独文件便于告警
- 文件写入走 `AsyncAppender`（`neverBlock`），不阻塞业务线程
- `dev/local` 输出控制台彩色日志；`test` 不落盘；`prod` 控制台 + 文件
- 可用环境变量覆盖：`LOG_HOME`、`LOG_MAX_HISTORY`、`LOG_TOTAL_CAP`

## 数据库约定

- Mapper 接口放 `com.biw.mapper`（由 `@MapperScan` 扫描），XML 放 `resources/mapper/**`
- 分页插件已装配（PostgreSQL 方言，单页上限 500 条）
- 逻辑删除字段约定为 `deleted`（0 未删 / 1 已删）
- 表结构变更：目前 `sql/init.sql` 只在空卷首次启动时执行；后续建议接 Flyway 或 Liquibase

## 版本管理约定

`AI agent` 产生的目录与文件一律不入库，已在 `.gitignore` 中排除：

`.pi/` `.claude/` `.cursor/` `.codex/` `.gemini/` `.agents/` `.openidea/` `.idea/`
`AGENTS.md` `CLAUDE.md` `GEMINI.md` `COPILOT.md` `.mcp.json` 等。

本地配置 `application-local.yml`、`.env`、`secrets/`、`logs/`、`target/` 同样忽略。

## 测试

```bash
mvn test
```

- `BiwApplicationContextTest`：完整上下文启动（Web + MyBatis-Plus + Sa-Token + Redis DAO，随机端口）
- `AuthControllerValidationTest`：参数校验与全局异常处理
