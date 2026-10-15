package com.light.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 启动真实网关和四个临时 HTTP 下游，验证 application.yml 路由；不依赖 Docker 或固定端口。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final Map<String, HttpServer> DOWNSTREAMS = new LinkedHashMap<>();

    @Autowired
    Environment environment;

    private final HttpClient client = HttpClient.newHttpClient();

    /** 与 JwtGlobalFilter 默认密钥一致，测试请求携带合法 Bearer Token 才能穿过全局鉴权 */
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor(
            "light-v2-development-secret-change-me-2026".getBytes(StandardCharsets.UTF_8));

    private static String bearerToken() {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder()
                .subject("1")
                .claim("username", "test")
                .claim("role", "USER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(1))))
                .signWith(JWT_KEY)
                .compact();
    }

    @DynamicPropertySource
    static void downstreamUrls(DynamicPropertyRegistry registry) throws IOException {
        // 路由测试使用临时 HTTP 服务，不依赖本机是否启动 Nacos。
        registry.add("spring.cloud.nacos.discovery.enabled", () -> "false");
        for (String service : new String[]{"identity", "knowledge", "file", "learning"}) {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String received = service + "|" + exchange.getRequestMethod() + "|"
                        + exchange.getRequestURI() + "|" + body;
                byte[] response = received.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.getResponseHeaders().set("X-Downstream", service);
                exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("/failure") ? 409 : 200,
                        response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
                exchange.close();
            });
            server.start();
            DOWNSTREAMS.put(service, server);
            // 与 application.yml 中 uri 上的 ${xxx_SERVICE_URL:lb://...} 占位符对应，
            // 直接覆盖 routes[i].uri 会让 Binder 用仅含 uri 的列表整体替换 YAML 列表，导致 predicates 丢失。
            registry.add(service + "_SERVICE_URL",
                    () -> "http://127.0.0.1:" + server.getAddress().getPort());
        }
    }

    @AfterAll
    static void stopDownstreams() {
        DOWNSTREAMS.values().forEach(server -> server.stop(0));
    }

    @Test
    void allConfiguredRoutesPreservePathAndQuery() throws Exception {
        for (String service : DOWNSTREAMS.keySet()) {
            var response = send("GET", "/api/" + service + "/probe?q=java", null);
            assertEquals(200, response.statusCode(), service);
            assertEquals(service, response.headers().firstValue("X-Downstream").orElseThrow());
            assertEquals(service + "|GET|/api/" + service + "/probe?q=java|", response.body());
        }
    }

    @Test
    void forwardsWriteBody() throws Exception {
        var response = send("POST", "/api/knowledge/entries", "{\"title\":\"Java\"}");
        assertEquals(200, response.statusCode());
        assertEquals("knowledge|POST|/api/knowledge/entries|{\"title\":\"Java\"}", response.body());
    }

    @Test
    void preservesDownstreamErrorStatus() throws Exception {
        assertEquals(409, send("GET", "/api/identity/failure", null).statusCode());
    }

    @Test
    void unmatchedPathIsNotForwarded() throws Exception {
        assertEquals(404, send("GET", "/api/unknown/probe", null).statusCode());
    }

    @Test
    void gatewayHealthIsServedLocally() throws Exception {
        var response = send("GET", "/actuator/health", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertFalse(response.headers().firstValue("X-Downstream").isPresent());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", bearerToken())
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
