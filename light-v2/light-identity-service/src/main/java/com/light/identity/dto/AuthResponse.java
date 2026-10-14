package com.light.identity.dto;

import com.light.identity.entity.UserAccount;

public record AuthResponse(String accessToken, String refreshToken, UserView user) {
    public record UserView(Long id, String username, String nickname, String role) {
        public static UserView from(UserAccount user) {
            return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        }
    }
}
