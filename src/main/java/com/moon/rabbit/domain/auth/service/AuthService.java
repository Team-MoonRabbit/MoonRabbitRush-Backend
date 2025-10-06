package com.moon.rabbit.domain.auth.service;

import com.moon.rabbit.domain.auth.dto.TokenResponse;
import com.moon.rabbit.domain.auth.entity.RefreshToken;
import com.moon.rabbit.domain.auth.repository.RefreshTokenRepository;
import com.moon.rabbit.domain.user.entity.User;
import com.moon.rabbit.domain.user.repository.UserRepository;
import com.moon.rabbit.global.exception.HttpException;
import com.moon.rabbit.global.security.jwt.JwtProvider;
import com.moon.rabbit.global.security.jwt.dto.JwtDetails;
import com.moon.rabbit.global.security.jwt.enums.JwtType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refreshTokenExpires}")
    private long refreshTokenTtl;

    public TokenResponse refresh(String refreshToken) {
        if(refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        if (!jwtProvider.validateToken(refreshToken, JwtType.REFRESH_TOKEN)) {
            throw new HttpException(HttpStatus.FORBIDDEN, "잘못된 토큰입니다.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new HttpException(HttpStatus.FORBIDDEN, "토큰이 존재하지 않습니다."));

        if (Instant.now().isAfter(storedToken.getExpiryDate())) {
            throw new HttpException(HttpStatus.FORBIDDEN, "토큰이 만료되었습니다.");
        }

        String  userId = storedToken.getUserId();
        JwtDetails newAccessToken = jwtProvider.generateToken(userId, JwtType.ACCESS_TOKEN);
        JwtDetails newRefreshToken = jwtProvider.generateToken(userId, JwtType.REFRESH_TOKEN);

        storedToken.setToken(newRefreshToken.token());
        storedToken.setExpiryDate(Instant.now().plusMillis(refreshTokenTtl));
        refreshTokenRepository.save(storedToken);

        return new TokenResponse(
                newAccessToken.token(),
                newAccessToken.expiredAt(),
                newRefreshToken.token(),
                newRefreshToken.expiredAt()
        );
    }

    public void logout(String refreshToken) {
        if(refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        if (!jwtProvider.validateToken(refreshToken, JwtType.REFRESH_TOKEN)) {
            throw new HttpException(HttpStatus.UNAUTHORIZED, "잘못된 리프레시 토큰입니다.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."));

        refreshTokenRepository.delete(storedToken);
    }
}