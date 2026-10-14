package com.light.identity.controller;

import com.light.common.auth.CurrentUser;
import com.light.common.result.Result;
import com.light.identity.dto.AuthResponse;
import com.light.identity.dto.LoginRequest;
import com.light.identity.dto.RefreshRequest;
import com.light.identity.dto.RegisterRequest;
import com.light.identity.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class AuthController {
    private final IdentityService identityService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(identityService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(identityService.login(request));
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(identityService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        CurrentUser.requireId();
        identityService.logout(request == null ? null : request.getRefreshToken());
        return Result.success();
    }

    @GetMapping("/me")
    public Result<AuthResponse.UserView> me() {
        return Result.success(identityService.me());
    }
}
