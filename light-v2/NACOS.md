# Nacos 本地启动

Nacos 是独立基础设施，不放进 `light-v2` 的 Maven 模块中。下载与当前 Spring Cloud Alibaba 兼容的 Nacos Server 解压后，在 Nacos 目录执行：

```powershell
.in\startup.cmd -m standalone
```

默认地址是 `http://127.0.0.1:8848/nacos`，默认开发账号通常是 `nacos / nacos`。如果本地 Nacos 修改过账号或端口，启动服务前设置：

```powershell
$env:NACOS_SERVER_ADDR = "127.0.0.1:8848"
$env:NACOS_USERNAME = "nacos"
$env:NACOS_PASSWORD = "nacos"
```

然后再启动 Gateway 和四个业务服务。它们会使用各自 `spring.application.name` 自动注册到 `DEFAULT_GROUP`：

| 服务 | 注册名 | 默认端口 |
|---|---|---:|
| Gateway | `light-gateway` | 8080 |
| 身份服务 | `light-identity-service` | 8101 |
| 知识服务 | `light-knowledge-service` | 8102 |
| 文件服务 | `light-file-service` | 8103 |
| 学习服务 | `light-learning-service` | 8104 |

Gateway 的路由使用 `lb://服务名`。请求先到 Gateway，再由 Nacos 提供实例地址，Spring Cloud LoadBalancer 选择实例并转发。

如果暂时没有启动 Nacos，可将 `NACOS_ENABLED=false`，但这时 `lb://` 路由无法发现业务服务，适合只启动单个服务做开发调试；完整链路应先启动 Nacos。
