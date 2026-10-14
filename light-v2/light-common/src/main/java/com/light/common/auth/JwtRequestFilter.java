package com.light.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 只负责解析令牌；具体接口通过 CurrentUser.requireId() 决定是否必须登录。 */
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        try {
            boolean publicEndpoint = request.getRequestURI().startsWith("/actuator/")
                    || "/api/identity/ping".equals(request.getRequestURI())
                    || (java.util.Set.of("/api/identity/login", "/api/identity/register",
                    "/api/identity/refresh").contains(request.getRequestURI())
                    && "POST".equals(request.getMethod()));
            if (publicEndpoint) {
                chain.doFilter(request, response);
                return;
            }
            try {
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Missing token");
                }
                CurrentUser.set(jwtService.principal(authorization.substring(7).trim()));
            } catch (RuntimeException ex) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\"}");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
