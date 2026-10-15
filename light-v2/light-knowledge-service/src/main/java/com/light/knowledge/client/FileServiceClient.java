package com.light.knowledge.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 文件服务客户端：knowledge 删除模块/条目时，跨服务通知 file 服务清理附件。
 *
 * - 通过 {@link LoadBalanced} RestClient 以服务名 light-file-service 访问，
 *   由 Nacos + LoadBalancer 解析实例，不写死 IP/端口
 * - 透传当前请求的 Authorization 头：file 服务同样要做用户鉴权和归属校验，
 *   不能用服务端特权绕过（避免水平越权）
 * - 清理失败只记日志不阻断主流程：知识数据删除优先，磁盘/元数据残留可人工排查，
 *   避免因文件服务短暂不可用导致用户删不掉笔记
 */
@Slf4j
@Component
public class FileServiceClient {

    private static final String FILE_SERVICE = "http://light-file-service";

    private final RestClient restClient;

    public FileServiceClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(FILE_SERVICE)
                .requestInterceptor((request, body, execution) -> {
                    String auth = currentAuthorization();
                    if (auth != null && !auth.isBlank()) {
                        request.getHeaders().set("Authorization", auth);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /** 删除某条目下的全部附件；任何异常都被吞掉并记录，调用方继续删除流程 */
    public void deleteAttachments(Long entryId) {
        try {
            restClient.delete()
                    .uri("/api/file/entry/{entryId}", entryId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("调用文件服务清理条目附件失败 entryId={}: {}", entryId, ex.getMessage());
        }
    }

    /** 删除单个文件（模块封面等 entryId 为空的文件） */
    public void deleteFile(Long fileId) {
        try {
            restClient.delete()
                    .uri("/api/file/{id}", fileId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("调用文件服务删除文件失败 fileId={}: {}", fileId, ex.getMessage());
        }
    }

    /** 从当前 Servlet 请求线程取 Authorization 头，透传给下游服务 */
    private String currentAuthorization() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                return sra.getRequest().getHeader("Authorization");
            }
        } catch (Exception ignored) {
            // 非请求线程（如定时任务）调用时无上下文
        }
        return null;
    }

    @Configuration
    static class RestClientConfig {
        @Bean
        @LoadBalanced
        RestClient.Builder loadBalancedRestClientBuilder() {
            return RestClient.builder();
        }
    }
}
