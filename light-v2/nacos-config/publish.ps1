# ============================================================
# 把 nacos-config/ 下的配置一键发布到 Nacos。
# 重建 nacos-data 数据卷后执行一次即可恢复全部中心配置。
# 适用 Nacos 3.x（v3 admin API，鉴权关闭时可直接调用）。
# 用法：
#   pwsh ./nacos-config/publish.ps1
#   pwsh ./nacos-config/publish.ps1 -NacosAddr http://localhost:8848
# ============================================================
param(
    [string]$NacosAddr = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

function Publish-Config {
    param(
        [string]$DataId,
        [string]$Group,
        [string]$Type,
        [string]$File
    )
    # 注意 Nacos 3.x 参数名是 groupName（2.x 为 group），接口前缀 /nacos/v3/admin/cs/config
    $resp = curl.exe -s -X POST "$NacosAddr/nacos/v3/admin/cs/config" `
        --data-urlencode "dataId=$DataId" `
        --data-urlencode "groupName=$Group" `
        --data-urlencode "type=$Type" `
        --data-urlencode "content@$File"
    if ($resp -notmatch '"code":0') {
        throw "发布失败: $DataId / $Group -> $resp"
    }
    Write-Host "published: $Group / $DataId" -ForegroundColor Green
}

# 全服务共享配置（DEFAULT_GROUP，与 spring.config.import 对应）
Publish-Config "application-common.yml" "DEFAULT_GROUP" "yaml" `
    (Join-Path $PSScriptRoot "application-common.yml")

# Sentinel 网关规则（SENTINEL_GROUP）
Publish-Config "light-gateway-gw-flow.json" "SENTINEL_GROUP" "json" `
    (Join-Path $PSScriptRoot "sentinel/light-gateway-gw-flow.json")
Publish-Config "light-gateway-degrade.json" "SENTINEL_GROUP" "json" `
    (Join-Path $PSScriptRoot "sentinel/light-gateway-degrade.json")

Write-Host "全部配置发布完成。" -ForegroundColor Cyan
