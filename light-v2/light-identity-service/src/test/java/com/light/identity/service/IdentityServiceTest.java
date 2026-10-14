package com.light.identity.service;

import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.identity.dto.AuthResponse;
import com.light.identity.dto.LoginRequest;
import com.light.identity.dto.RefreshRequest;
import com.light.identity.dto.RegisterRequest;
import com.light.identity.entity.RefreshToken;
import com.light.identity.entity.UserAccount;
import com.light.identity.mapper.RefreshTokenMapper;
import com.light.identity.mapper.UserAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock UserAccountMapper userMapper;
    @Mock RefreshTokenMapper refreshTokenMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks IdentityService service;

    @BeforeEach
    void setUp() {
        // @Value 字段在纯单元测试中不注入，手动设置
        ReflectionTestUtils.setField(service, "refreshTtl", Duration.ofDays(14));
        ReflectionTestUtils.setField(service, "initialPassword", "123456");
        // 预注册实体的 TableInfo，否则 LambdaQueryWrapper/LambdaUpdateWrapper 抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, UserAccount.class);
        TableInfoHelper.initTableInfo(assistant, RefreshToken.class);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private UserAccount user() {
        UserAccount u = new UserAccount();
        u.setId(1L);
        u.setUsername("light");
        u.setPasswordHash("encoded-hash");
        u.setNickname("Light");
        u.setRole("USER");
        u.setEnabled(true);
        return u;
    }

    @Test
    void register_whenUsernameExists_shouldThrowConflict() {
        when(userMapper.selectOne(any())).thenReturn(user());

        RegisterRequest req = new RegisterRequest();
        req.setUsername("light");
        req.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(req));
        assertEquals(409, ex.getErrorCode().getCode());
        verify(userMapper, never()).insert(any(UserAccount.class));
    }

    @Test
    void register_success_shouldCreateUserAndIssueTokens() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        // MyBatis-Plus insert 会回填自增/雪花主键，这里用 Answer 模拟
        when(userMapper.insert(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });
        when(jwtService.issueAccessToken(1L, "light", "USER")).thenReturn("access-token");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("light");
        req.setPassword("123456");
        req.setNickname("Light");

        AuthResponse resp = service.register(req);

        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
        assertEquals("light", resp.user().username());
        assertEquals("USER", resp.user().role());
        verify(userMapper).insert(any(UserAccount.class));
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
    }

    @Test
    void login_wrongPassword_shouldThrowUnauthorized() {
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("light");
        req.setPassword("wrong");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void login_disabledUser_shouldThrowUnauthorized() {
        UserAccount disabled = user();
        disabled.setEnabled(false);
        when(userMapper.selectOne(any())).thenReturn(disabled);

        LoginRequest req = new LoginRequest();
        req.setUsername("light");
        req.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void login_success_shouldReturnAuthResponse() {
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.issueAccessToken(1L, "light", "USER")).thenReturn("access-token");

        LoginRequest req = new LoginRequest();
        req.setUsername("light");
        req.setPassword("123456");

        AuthResponse resp = service.login(req);

        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
    }

    @Test
    void refresh_invalidToken_shouldThrowUnauthorized() {
        when(refreshTokenMapper.selectOne(any())).thenReturn(null);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("unknown-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void refresh_revokedToken_shouldThrowUnauthorized() {
        RefreshToken revoked = new RefreshToken();
        revoked.setId(1L);
        revoked.setUserId(1L);
        revoked.setTokenHash("hash");
        revoked.setExpiresAt(LocalDateTime.now().plusDays(14));
        revoked.setRevokedAt(LocalDateTime.now());
        when(refreshTokenMapper.selectOne(any())).thenReturn(revoked);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("used-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void refresh_expiredToken_shouldThrowUnauthorized() {
        RefreshToken expired = new RefreshToken();
        expired.setId(1L);
        expired.setUserId(1L);
        expired.setTokenHash("hash");
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // 已过期
        when(refreshTokenMapper.selectOne(any())).thenReturn(expired);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("expired-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void refresh_success_shouldRotateToken() {
        RefreshToken active = new RefreshToken();
        active.setId(1L);
        active.setUserId(1L);
        active.setTokenHash("hash");
        active.setExpiresAt(LocalDateTime.now().plusDays(14));
        when(refreshTokenMapper.selectOne(any())).thenReturn(active);
        when(userMapper.selectById(1L)).thenReturn(user());
        // CAS 更新：revoked_at IS NULL 且匹配 id，更新 1 行
        when(refreshTokenMapper.update(any(), any())).thenReturn(1);
        when(jwtService.issueAccessToken(1L, "light", "USER")).thenReturn("new-access-token");

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-token");

        AuthResponse resp = service.refresh(req);

        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
        verify(refreshTokenMapper).update(any(), any());
    }

    @Test
    void refresh_raceCondition_shouldThrowWhenConsumed() {
        RefreshToken active = new RefreshToken();
        active.setId(1L);
        active.setUserId(1L);
        active.setTokenHash("hash");
        active.setExpiresAt(LocalDateTime.now().plusDays(14));
        when(refreshTokenMapper.selectOne(any())).thenReturn(active);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(refreshTokenMapper.update(any(), any())).thenReturn(0); // 已被并发请求消费

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh(req));
        assertEquals(401, ex.getErrorCode().getCode());
    }

    @Test
    void logout_shouldRevokeToken() {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setTokenHash("hash");
        token.setRevokedAt(null);
        when(refreshTokenMapper.selectOne(any())).thenReturn(token);

        service.logout("refresh-token");

        verify(refreshTokenMapper).updateById(token);
        assertNotNull(token.getRevokedAt());
    }

    @Test
    void me_shouldReturnCurrentUserView() {
        CurrentUser.set(new JwtService.UserPrincipal(1L, "light", "USER"));
        when(userMapper.selectById(1L)).thenReturn(user());

        AuthResponse.UserView view = service.me();

        assertEquals("light", view.username());
        assertEquals("Light", view.nickname());
        assertEquals("USER", view.role());
    }

    @Test
    void ensureDefaultUser_whenMissing_shouldCreateFixedUser() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-hash");

        service.ensureDefaultUser();

        verify(userMapper).insert(any(UserAccount.class));
    }
}
