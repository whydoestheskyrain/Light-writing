package com.light.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {
    private final javax.crypto.SecretKey key;
    private static final Set<String> PUBLIC = Set.of("/api/identity/login", "/api/identity/register", "/api/identity/refresh");
    public JwtGlobalFilter(@Value("${security.jwt.secret:light-v2-development-secret-change-me-2026}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT密钥至少32字节");
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    @Override public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path=exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator/") || "/api/identity/ping".equals(path)
                || (PUBLIC.contains(path) && "POST".equalsIgnoreCase(exchange.getRequest().getMethod().name()))) return chain.filter(exchange);
        String header=exchange.getRequest().getHeaders().getFirst("Authorization");
        try { if(header==null||!header.startsWith("Bearer ")) throw new IllegalArgumentException(); Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7).trim()); return chain.filter(exchange); }
        catch (RuntimeException ex) { ServerHttpResponse response=exchange.getResponse(); response.setStatusCode(HttpStatus.UNAUTHORIZED); response.getHeaders().add("Content-Type","application/json;charset=UTF-8"); byte[] body="{\"code\":401,\"msg\":\"未登录或登录已过期\"}".getBytes(StandardCharsets.UTF_8); return response.writeWith(Mono.just(response.bufferFactory().wrap(body))); }
    }
    @Override public int getOrder(){return -100;}
}
