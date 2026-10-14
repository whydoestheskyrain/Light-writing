package com.light.identity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;
import com.light.identity.dto.AuthResponse;
import com.light.identity.dto.LoginRequest;
import com.light.identity.dto.RefreshRequest;
import com.light.identity.dto.RegisterRequest;
import com.light.identity.entity.RefreshToken;
import com.light.identity.entity.UserAccount;
import com.light.identity.mapper.RefreshTokenMapper;
import com.light.identity.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserAccountMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${security.jwt.refresh-ttl:P14D}")
    private Duration refreshTtl;
    @Value("${INITIAL_USER_PASSWORD:123456}")
    private String initialPassword;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserAccount existing = findByUsername(request.getUsername());
        if (existing != null) throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setEnabled(true);
        userMapper.insert(user);
        return issue(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = findByUsername(request.getUsername());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return issue(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.getRefreshToken());
        RefreshToken old = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hash));
        if (old == null || old.getRevokedAt() != null || old.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        UserAccount user = userMapper.selectById(old.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int consumed = refreshTokenMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RefreshToken>()
                        .eq(RefreshToken::getId, old.getId()).isNull(RefreshToken::getRevokedAt)
                        .set(RefreshToken::getRevokedAt, LocalDateTime.now()));
        if (consumed != 1) throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌已使用");
        return issue(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        RefreshToken token = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, sha256(refreshToken)));
        if (token != null && token.getRevokedAt() == null) {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
        }
    }

    public AuthResponse.UserView me() {
        UserAccount user = userMapper.selectById(CurrentUser.requireId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return AuthResponse.UserView.from(user);
    }

    @Transactional
    public void ensureDefaultUser() {
        if (findByUsername("light") != null) return;
        UserAccount user = new UserAccount();
        user.setUsername("light");
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setId(1L);
        user.setNickname("Light");
        user.setRole("USER");
        user.setEnabled(true);
        userMapper.insert(user);
    }

    private AuthResponse issue(UserAccount user) {
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(sha256(refresh));
        token.setExpiresAt(LocalDateTime.now().plus(refreshTtl));
        refreshTokenMapper.insert(token);
        return new AuthResponse(access, refresh, AuthResponse.UserView.from(user));
    }

    private UserAccount findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username).last("LIMIT 1"));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK不支持SHA-256", e);
        }
    }
}
