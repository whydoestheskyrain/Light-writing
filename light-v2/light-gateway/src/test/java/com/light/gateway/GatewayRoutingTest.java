package com.light.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
            registry.add(service + "_SERVICE_URL",
                    () -> "http://127.0.0.1:" + server.getAddress().getPort());
        }
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri",
                () -> DOWNSTREAMS.get("identity").getAddress().getPort() == 0 ? "http://localhost:1"
                        : "http://127.0.0.1:" + DOWNSTREAMS.get("identity").getAddress().getPort());
        registry.add("spring.cloud.gateway.server.webflux.routes[1].uri",
                () -> "http://127.0.0.1:" + DOWNSTREAMS.get("knowledge").getAddress().getPort());
        registry.add("spring.cloud.gateway.server.webflux.routes[2].uri",
                () -> "http://127.0.0.1:" + DOWNSTREAMS.get("file").getAddress().getPort());
        registry.add("spring.cloud.gateway.server.webflux.routes[3].uri",
                () -> "http://127.0.0.1:" + DOWNSTREAMS.get("learning").getAddress().getPort());
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
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
