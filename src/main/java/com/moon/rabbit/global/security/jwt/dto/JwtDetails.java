package com.moon.rabbit.global.security.jwt.dto;
import java.time.LocalDateTime;

public record JwtDetails(
        String token,
        LocalDateTime expiredAt
) {}