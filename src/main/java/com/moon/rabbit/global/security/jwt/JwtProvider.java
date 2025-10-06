package com.moon.rabbit.global.security.jwt;

import com.moon.rabbit.global.exception.HttpException;
import com.moon.rabbit.global.security.details.AuthDetailsService;
import com.moon.rabbit.global.security.jwt.dto.JwtDetails;
import com.moon.rabbit.global.security.jwt.enums.JwtType;
import com.moon.rabbit.global.util.DateUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private final AuthDetailsService authDetailsService;
    private final JwtProperties jwtProperties;

    public JwtProvider(AuthDetailsService authDetailsService, JwtProperties jwtProperties) {
        this.authDetailsService = authDetailsService;
        this.jwtProperties = jwtProperties;
    }

    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        String resolvedToken = resolveToken(token);
        Claims payload = getPayload(resolvedToken, JwtType.ACCESS_TOKEN);

        UserDetails userDetails = authDetailsService.loadUserByUsername(payload.getSubject());

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public boolean validateToken(String token, JwtType jwtType) {
        try {
            getPayload(token, jwtType);
            return true;
        } catch (HttpException e) {
            return false;
        }
    }

    public String resolveToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        return token.substring(7);
    }

    public String getIdByRefreshToken(String refreshToken) {
        return getPayload(refreshToken, JwtType.REFRESH_TOKEN).getSubject();
    }

    public Claims getPayload(String token, JwtType jwtType) {
        if (token == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "토큰이 존재하지 않습니다.");
        }

        String tokenKey = jwtType == JwtType.ACCESS_TOKEN
                ? jwtProperties.getAccessTokenKey()
                : jwtProperties.getRefreshTokenKey();

        byte[] keyBytes = Base64.getEncoder().encode(tokenKey.getBytes());
        var signingKey = Keys.hmacShaKeyFor(keyBytes);
        try {
            return Jwts
                    .parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new HttpException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException e) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "지원하지 않는 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "잘못된 형식의 토큰입니다.");
        } catch (RuntimeException e) {
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "기타 JWT 토큰 오류입니다.");
        }
    }


    public JwtDetails generateToken(String id, JwtType jwtType) {
        long tokenExpires = jwtType == JwtType.ACCESS_TOKEN
                ? jwtProperties.getAccessTokenExpires()
                : jwtProperties.getRefreshTokenExpires();

        LocalDateTime expiredAt = LocalDateTime.now().plus(Duration.ofMillis(tokenExpires));

        String tokenKey = jwtType == JwtType.ACCESS_TOKEN
                ? jwtProperties.getAccessTokenKey()
                : jwtProperties.getRefreshTokenKey();

        byte[] keyBytes = Base64.getEncoder().encode(tokenKey.getBytes());
        var signingKey = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts
                .builder()
                .subject(String.valueOf(id))
                .signWith(signingKey)
                .issuedAt(new Date())
                .expiration(DateUtil.toDate(expiredAt))
                .compact();

        return new JwtDetails(token, expiredAt);
    }
}