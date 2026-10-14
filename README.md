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
| 注册中心 | Nacos 2.4（服务发现 + `lb://` 负载均衡） |
| 网关 | Spring Cloud Gateway（WebFlux 反应式） |
| 基础设施 | Docker Compose（MySQL / Redis / Nacos / 5 个应用容器一键启动） |

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

启动 8 个容器：MySQL、Redis、Nacos + Gateway 与 4 个业务服务。
首次启动会自动建表（各服务 `classpath:db/schema.sql`）并创建默认用户。

验证：

```bash
curl http://localhost:8080/api/identity/ping
# {"code":0,"data":{"service":"identity",...}}
```

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
| POST | `/api/learning/heartbeat` | 上报学习时长 |
| GET | `/api/learning/summary?range=today` | 学习汇总 |

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
