package com.light.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** 统一签发和解析 JWT。业务服务共享同一个密钥，才能验证 Gateway 转发的令牌。 */
@Component
public class JwtService {
    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(
            @Value("${security.jwt.secret:light-v2-development-secret-change-me-2026}") String secret,
            @Value("${security.jwt.access-ttl:PT2H}") Duration accessTtl) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret 至少需要 32 个字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = accessTtl;
    }

    public String issueAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public UserPrincipal principal(String token) {
        Claims claims = parse(token);
        return new UserPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class));
    }

    public record UserPrincipal(Long id, String username, String role) {}
}
