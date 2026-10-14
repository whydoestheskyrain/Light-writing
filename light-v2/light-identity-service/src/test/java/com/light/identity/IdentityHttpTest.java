package com.light.identity;

import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 HTTP 验证公共异常处理是否被身份服务加载；测试接口只存在于测试目录。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IdentityHttpTest.FailureConfig.class)
class IdentityHttpTest {

    @Autowired
    Environment environment;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void pingReturnsServiceIdentity() throws Exception {
        var response = send("GET", "/api/identity/ping", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"service\":\"identity\""));
        assertTrue(response.body().contains("\"code\":0"));
    }

    @Test
    void businessFailureUsesSharedErrorContract() throws Exception {
        var response = send("GET", "/test/business", null);
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"code\":404"));
        assertTrue(response.body().contains("missing-user"));
    }

    @Test
    void invalidBodyReturnsBadRequest() throws Exception {
        var response = send("POST", "/test/validation", "{\"name\":\"\"}");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"code\":400"));
        assertTrue(response.body().contains("name:"));
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() throws Exception {
        var response = send("GET", "/test/system", null);
        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("\"code\":500"));
        assertFalse(response.body().contains("internal-secret"));
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureConfig {
        @Bean
        FailureController failureController() {
            return new FailureController();
        }
    }

    @RestController
    static class FailureController {
        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "missing-user");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody NameRequest request) { }

        @GetMapping("/test/system")
        void system() {
            throw new IllegalStateException("internal-secret");
        }
    }

    record NameRequest(@NotBlank String name) { }
}
