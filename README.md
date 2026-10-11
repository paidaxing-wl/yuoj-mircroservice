# YUOJ 在线判题系统（微服务版）

基于 Spring Cloud 微服务架构的在线判题（Online Judge）后端，支持题目管理、提交判题、用户管理与网关统一鉴权。

> 本项目为微服务拆分版本，按业务域划分为独立服务，通过 Nacos 注册发现、OpenFeign 服务调用、RabbitMQ 异步判题串联。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 基础框架 | Java 8、Spring Boot 2.6.13 |
| 微服务 | Spring Cloud 2021.0.5、Spring Cloud Alibaba 2021.0.5.0 |
| 注册中心 | Nacos 2.x |
| 服务调用 | OpenFeign + LoadBalancer |
| 网关 | Spring Cloud Gateway |
| 限流熔断 | Sentinel |
| 持久层 | MyBatis-Plus 3.5.2、MySQL |
| 缓存 / 会话 | Redis、Spring Session |
| 消息队列 | RabbitMQ |
| 接口文档 | Knife4j（Swagger2 聚合） |
| 判题沙箱 | 远程代码沙箱（独立项目，通过 HTTP 调用） |

## 模块说明

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `yuoj-backend-gateway` | 8101 | 统一入口、路由转发、跨域、全局权限校验 |
| `yuoj-backend-user-service` | 8102 | 用户注册/登录/权限管理 |
| `yuoj-backend-question-service` | 8103 | 题目与题目提交管理，提交后投递判题消息 |
| `yuoj-backend-judge-service` | 8104 | 消费判题消息、调用代码沙箱、回写判题结果 |
| `yuoj-backend-common` | - | 公共常量、异常、统一响应、工具类 |
| `yuoj-backend-model` | - | 实体、DTO、VO、枚举、沙箱模型 |
| `yuoj-backend-service-client` | - | Feign 客户端定义（内部服务调用契约） |

## 环境准备

启动前需要保证以下依赖服务已就绪：

- **JDK 8**
- **MySQL 8**：创建数据库 `yuoj`，导入建表 SQL
- **Redis**：默认 `localhost:6379`
- **Nacos**：默认 `localhost:8848`，单机模式启动
- **RabbitMQ**：默认 `localhost:5672`
- **远程代码沙箱**：`codesandbox.type` 设为 `remote`，需另行启动代码沙箱服务

## 配置说明（重要）

**本项目不提交任何数据库、Redis 等真实密码。** 各服务的 `application.yml` 通过环境变量读取敏感配置：

| 环境变量 | 说明 | 是否必需 |
| --- | --- | --- |
| `MYSQL_USERNAME` | MySQL 用户名，默认 `root` | 否 |
| `MYSQL_PASSWORD` | MySQL 密码 | **必需** |
| `REDIS_PASSWORD` | Redis 密码 | **必需** |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名，默认 `guest` | 否 |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码，默认 `guest` | 否 |

> 未提供 `MYSQL_PASSWORD` / `REDIS_PASSWORD` 时服务会启动失败，这是刻意设计——避免把弱密码写进仓库。

### Windows（PowerShell）

```powershell
$env:MYSQL_PASSWORD = "你的数据库密码"
$env:REDIS_PASSWORD = "你的 Redis 密码"
```

### macOS / Linux

```bash
export MYSQL_PASSWORD="你的数据库密码"
export REDIS_PASSWORD="你的 Redis 密码"
```

### IntelliJ IDEA

`Run/Debug Configurations` → 对应服务的 `Environment variables` 中填入上述变量即可，无需改动配置文件。

## 本地启动

按依赖顺序启动（先启动 Nacos、MySQL、Redis、RabbitMQ）：

```bash
# 根目录构建
mvn clean install -DskipTests

# 依次启动各个服务（可分别在 IDEA 中运行，或使用命令行）
mvn -pl yuoj-backend-gateway spring-boot:run
mvn -pl yuoj-backend-user-service spring-boot:run
mvn -pl yuoj-backend-question-service spring-boot:run
mvn -pl yuoj-backend-judge-service spring-boot:run
```

启动完成后：

- 统一入口：`http://localhost:8101`
- 接口文档（网关聚合）：`http://localhost:8101/doc.html`

## 接口路由

请求统一经网关（8101）转发，按路径前缀路由到对应服务：

| 路径前缀 | 目标服务 |
| --- | --- |
| `/api/user/**` | `yuoj-backend-user-service` |
| `/api/question/**` | `yuoj-backend-question-service` |
| `/api/judge/**` | `yuoj-backend-judge-service` |

## 判题流程

1. 用户在 `question-service` 提交代码，生成提交记录；
2. 提交记录状态置为「待判题」，通过 RabbitMQ 投递判题消息；
3. `judge-service` 消费消息，取出题目用例与代码；
4. 调用远程代码沙箱执行，获得输出结果；
5. 按判题策略（默认策略 / Java 专用策略）比对期望输出；
6. 回写判题状态与判题信息（通过/答案错误/编译错误/超时等）。

## 目录结构

```
yuoj-backend-microservice
├── yuoj-backend-common          # 公共模块：常量、异常、统一响应、工具类
├── yuoj-backend-model           # 数据模型：entity / dto / vo / enums
├── yuoj-backend-service-client  # Feign 客户端契约
├── yuoj-backend-gateway         # 网关：路由、跨域、全局鉴权过滤器
├── yuoj-backend-user-service    # 用户服务
├── yuoj-backend-question-service# 题目服务（含消息生产者）
└── yuoj-backend-judge-service   # 判题服务（含消息消费者、判题策略）
```

## License

仅供学习交流使用。
