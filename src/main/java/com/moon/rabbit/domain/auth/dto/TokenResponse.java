package com.moon.rabbit.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private final String accessToken;
    private final LocalDateTime accessTokenExpiredAt;
    private final String refreshToken;
    private final LocalDateTime refreshTokenExpiredAt;
}
