# Light Writing · 个人学习知识库

面向程序员与计算机专业学生的个人学习知识库：整理知识模块、记录笔记与算法题、管理八股与链接、上传学习资料，并持续追踪学习时长与复习进度。支持多账号、用户数据隔离，后续可演进为知识分享与付费订阅平台。

本仓库为 2.0 版本：后端采用 Spring Boot 4.1 微服务架构，前端为 Vue 3 + TypeScript 单页应用。

## 功能特性

- **账号体系**：注册 / 登录 / JWT 双令牌认证（access 2h + refresh 14d，refresh 一次性轮换）
- **数据隔离**：模块、条目、文件、学习记录全部按用户归属，越权访问返回 401/404
- **知识管理**：学习模块（含封面图）、条目 CRUD、分页检索、标签、学习状态（未做 / 在做 / 已掌握）
- **资料管理**：图片 / PDF / 代码文件上传，SHA-256 校验，文件元数据与笔记关联
- **学习追踪**：前端心跳上报学习时长，后端持久化（事件 / 每日 / 模块三张统计表），支持汇总查询
- **富文本**：Markdown 编辑器、图片上传、目录导航、代码一键复制
- **安全下载**：受保护文件经带 JWT 的 blob 通道访问，支持 access token 过期静默刷新

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vite 5、Pinia、Vue Router、Element Plus、md-editor-v3、ECharts、Axios |
| 后端 | Java 25 (LTS)、Spring Boot 4.1.x、Spring Framework 7、Spring Cloud 2025.1.x、Spring Cloud Alibaba 2025.1.x |
| 安全 | JJWT 0.12、BCrypt 密码加密、网关统一鉴权过滤器 |
| 数据 | MySQL 8.4、MyBatis-Plus 3.5、Redis 7 |
| 注册 / 配置中心 | Nacos 3.2（服务发现 + 配置中心 + `lb://` 负载均衡） |
| 网关 | Spring Cloud Gateway（WebFlux 反应式） |
| 限流熔断 | Sentinel 1.8（规则外置 Nacos，动态热更新） |
| 监控 | Micrometer + Prometheus + Grafana |
| 基础设施 | Docker Compose（MySQL / Redis / Nacos / 5 应用 / Prometheus / Grafana 一键启动） |

### 版本清单

以下版本号为当前构建实际解析的版本（`mvn dependency:list` / `npm ls` 核实），非声明范围。

**本机开发环境**

| 工具 | 版本 |
|---|---|
| JDK | Temurin OpenJDK 25.0.4.1 LTS |
| Maven | 3.9.9 |
| Node.js / npm | 24.14.1 / 11.11.0 |
| Docker Engine / Compose | 29.7.2 / 5.4.0（WSL2 后端） |
| Git | 2.54 |

**后端框架与关键依赖（light-v2）**

| 组件 | 版本 |
|---|---|
| Java（编译 release） | 25 |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Cloud | 2025.1.3 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Spring Cloud Gateway（WebFlux） | 5.0.3 |
| Spring Cloud Commons / LoadBalancer | 5.0.3 |
| Nacos Client | 3.1.1 |
| Sentinel Core | 1.8.9 |
| MyBatis-Plus（`mybatis-plus-spring-boot4-starter`） | 3.5.16 |
| JJWT | 0.12.6 |
| Spring Security Crypto（仅 BCrypt） | 7.1.1 |
| MySQL Connector/J | 9.7.0 |
| Spring Data Redis / Lettuce | 4.1.1 / 7.5.2 |
| 内嵌 Tomcat | 11.0.24 |
| Reactor Core | 3.8.7 |
| Jackson | 3.1.5（Boot 4 起包名迁移为 `tools.jackson`） |
| Lombok | 1.18.46 |

**前端依赖（Light F）**

| 组件 | 版本 |
|---|---|
| Vue | 3.4.x |
| TypeScript | 5.5.x |
| Vite / vue-tsc | 5.3.x / 2.0.x |
| Pinia / Vue Router | 2.1.x / 4.4.x |
| Element Plus | 2.7.x |
| Axios | 1.7.x |
| md-editor-v3 / markdown-it | 4.17.x / 14.1.x |
| ECharts | 6.1.x |
| Sass | 1.77.x |

**基础设施镜像（docker-compose.yml）**

| 容器 | 镜像版本 | 宿主端口 |
|---|---|---|
| MySQL | mysql:8.4（LTS） | 3306 |
| Redis | redis:7-alpine | 6379 |
| Nacos | nacos/nacos-server:v3.2.4（standalone，关闭鉴权） | 8848 / 9848 / 9849 |
| 5 个应用服务 | maven:3.9-temurin-25 构建 → eclipse-temurin:25-jre 运行（多阶段） | 仅 gateway 8080 |
| Prometheus | prom/prometheus:v2.54.1 | 9090 |
| Grafana | grafana/grafana:11.2.0（admin/admin） | 3000 |

**v1 → v2 版本演进**

| 维度 | v1（已退役，本地保留） | v2（当前） |
|---|---|---|
| Java | 8 | 25 LTS |
| Spring Boot | 2.7.18 | 4.1.1 |
| 架构 | 单体、无用户体系 | 6 模块微服务 + Nacos + Gateway |
| MyBatis-Plus | 3.4.3.4 | 3.5.16（boot4 starter） |
| 认证 | 无 | JWT 双令牌 + Sentinel 限流熔断 |
| 学习数据 | 浏览器 localStorage | 后端三表 + Redis 增量 |
| 部署 | 单 jar | Compose 10 容器（含监控栈） |

## 系统架构

```
浏览器 (Vue SPA, :5173)
        │  /api/**（Vite 代理）
        ▼
  API Gateway (:8080)          JWT 全局校验 · CORS · 路由分发
        │ lb://服务名（Nacos 服务发现 + LoadBalancer）
        ├── identity-service  (:8101)  用户、登录、刷新令牌
        ├── knowledge-service (:8102)  模块、条目、标签
        ├── file-service      (:8103)  文件上传、元数据、下载
        └── learning-service  (:8104)  学习时长、统计
                    │
        MySQL 8.4（各服务独立 schema.sql，按 user_id 隔离）
```

- 每个服务是独立 Spring Boot 应用，拥有独立进程、独立端口、独立数据表边界
- 只有 Gateway 对宿主机暴露 8080 端口，其余服务仅在 Docker 内网互通
- 服务间地址通过 Nacos 注册名解析，不写死 IP

## 目录结构

```
.
├── light-v2/                  # 2.0 微服务后端
│   ├── pom.xml                # 父 POM（Spring Boot 4.1.1 + Cloud BOM + Java 25）
│   ├── docker-compose.yml     # 全栈一键启动（mysql/redis/nacos/5 应用）
│   ├── light-common/          # 公共模块：统一 Result、异常、JWT 过滤器
│   ├── light-gateway/         # API 网关
│   ├── light-identity-service/
│   ├── light-knowledge-service/
│   ├── light-file-service/
│   └── light-learning-service/
├── Light F/                   # Vue 3 前端
│   ├── src/api/               # Axios 封装（token 注入、refresh 单飞重放）
│   ├── src/utils/fileUrl.ts   # 受保护文件 blob 访问
│   ├── src/stores/            # Pinia（模块、学习计时）
│   └── src/views/             # 页面（登录、首页、模块、条目、搜索等）
└── deploy/                    # v1 版本的 Docker 部署配置（归档保留）
```

## 快速开始

### 环境要求

- JDK 25、Maven 3.9+
- Docker Desktop
- Node.js 18+

### 1. 启动后端（Docker Compose）

```bash
cd light-v2
docker compose up -d --build
```

启动 10 个容器：MySQL、Redis、Nacos、Prometheus、Grafana + Gateway 与 4 个业务服务。
首次启动会自动建表（各服务 `classpath:db/schema.sql`）并创建默认用户。

Nacos 数据卷为空时（首次启动或执行过 `docker compose down -v`），需要灌入一份中心配置：

```powershell
pwsh ./nacos-config/publish.ps1
# 发布 application-common.yml（共享配置）与两份 Sentinel 规则到 Nacos
```

验证：

```bash
curl http://localhost:8080/api/identity/ping
# {"code":0,"data":{"service":"identity",...}}
```

### 监控与控制台

| 入口 | 地址 | 说明 |
|---|---|---|
| 应用 | http://localhost:5173 | 前端页面（light / 123456） |
| 网关 | http://localhost:8080 | 唯一业务入口 |
| Nacos 控制台 | http://localhost:8088 | Nacos 3.x 控制台独立在容器 8080（映射宿主 8088）；8848 仅为 API 端口 |
| Prometheus | http://localhost:9090 | Status → Targets 查看 5 个服务抓取状态 |
| Grafana | http://localhost:3000 | admin / admin，Prometheus 数据源已自动配好 |

Sentinel 限流 / 熔断规则在 Nacos 控制台「配置管理 → SENTINEL_GROUP」中维护：
`light-gateway-gw-flow.json`（各路由 QPS）、`light-gateway-degrade.json`（异常比例熔断），发布后秒级热更新，无需重启网关。

### 2. 启动前端

```bash
cd "Light F"
npm install
npm run dev
```

访问 http://localhost:5173 ，Vite 会把 `/api/**` 代理到网关 `localhost:8080`。

### 默认账号

| 用户名 | 密码 |
|---|---|
| light | 123456 |

也可在登录页自行注册新账号。

### 配置说明

后端配置均支持环境变量覆盖（见 `light-v2/.env.example`）：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | root123 | MySQL root 密码 |
| `JWT_SECRET` | 开发占位值 | 生产环境必须覆盖，至少 32 字节 |
| `DB_HOST` / `NACOS_SERVER_ADDR` | mysql / nacos:8848 | 容器网络内的服务名 |

## 主要 API

所有请求经网关 `http://localhost:8080`，除登录/注册/刷新外均需 `Authorization: Bearer <accessToken>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/identity/register` | 注册 |
| POST | `/api/identity/login` | 登录，返回 access/refresh token |
| POST | `/api/identity/refresh` | 刷新令牌（旧 refresh 立即作废） |
| GET | `/api/identity/me` | 当前用户信息 |
| GET/POST/PUT/DELETE | `/api/module/**` | 模块管理 |
| GET/POST/PUT/DELETE | `/api/entry/**` | 条目管理（含 `/tags`、`PATCH /{id}/status`） |
| POST | `/api/file/upload` | 上传文件（multipart/form-data） |
| GET | `/api/file/{id}/download` | 下载文件（需 JWT，前端走 blob 通道） |
| POST | `/api/learning/heartbeat` | 上报学习时长（eventId 幂等、按秒数 INCRBY） |
| GET | `/api/learning/summary?range=today` | 学习汇总（合并 Redis 未刷库增量，实时可见） |
| GET | `/api/learning/modules?range=all` | 按模块聚合学习时长（饼图） |
| DELETE | `/api/file/entry/{entryId}` | 删除条目下全部附件（knowledge 级联调用） |

## 开发约定

- **统一返回结构**：`{ code: 0, msg: "ok", data: ..., timestamp }`，非 0 为业务错误；HTTP 状态码与错误语义对齐
- **受保护文件**：数据库与 Markdown 中只保存稳定路径 `/api/file/{id}/download`；展示时前端带 token 拉取 blob URL，按 fileId 缓存，禁止把下载接口加入匿名白名单
- **令牌刷新**：access 过期后拦截器自动用 refresh 换新并单飞重放原请求；refresh 失效才跳转登录页
- **服务边界**：各服务不共享业务表，跨服务只通过 Nacos 注册的 HTTP API 协作

## 技术备注

- Spring Boot 4.1 起 Maven 需显式声明 Lombok 的 `annotationProcessorPaths`，否则 `@Data`/`@Slf4j` 静默失效
- Spring Cloud 2025.1 网关路由属性前缀为 `spring.cloud.gateway.server.webflux.routes`（旧前缀已被忽略）
- `lb://` 路由必须引入 `spring-cloud-starter-loadbalancer`，否则网关返回 503
- 容器内访问其它容器一律使用服务名（如 `mysql`、`nacos`），不能使用 `127.0.0.1`
- Nacos 服务端必须用 3.x：SCA 2025.1 自带 nacos-client 3.1.1，其配置模块走 v3 协议，对接 2.x 服务端时配置查询静默返回空（服务发现却正常，极易误判）
- 多阶段 Docker 构建在容器内用 Maven 打包，已通过 `docker/maven-settings.xml` 指定阿里云镜像，避免直连 Maven Central 握手中断
