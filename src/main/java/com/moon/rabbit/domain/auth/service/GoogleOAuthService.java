package com.moon.rabbit.domain.auth.service;

import com.moon.rabbit.domain.auth.dto.GoogleUserInfo;
import com.moon.rabbit.domain.auth.dto.TokenResponse;
import com.moon.rabbit.domain.auth.entity.RefreshToken;
import com.moon.rabbit.domain.auth.repository.RefreshTokenRepository;
import com.moon.rabbit.domain.user.entity.User;
import com.moon.rabbit.domain.user.repository.UserRepository;
import com.moon.rabbit.global.security.jwt.JwtProvider;
import com.moon.rabbit.global.security.jwt.dto.JwtDetails;
import com.moon.rabbit.global.security.jwt.enums.JwtType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final WebClient webClient;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${jwt.refreshTokenExpires}")
    private long refreshTokenTtl;
    @Value("${spring.security.oauth2.client.provider.google.authorization-uri}")
    private String authUrl;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUrl;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public String buildGoogleLoginUrl() {
        return authUrl + "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=email";
    }
    public TokenResponse loginWithCode(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        var tokenResponse = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();

        String accessToken = (String) tokenResponse.get("access_token");

        GoogleUserInfo googleUser = webClient.get()
                .uri(userInfoUrl)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();

        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> {
                    if (!googleUser.email().endsWith("@gsm.hs.kr")) {
                        throw new IllegalArgumentException("허용되지 않은 이메일 도메인입니다.");
                    }
                    return userRepository.save(
                            User.builder()
                                    .email(googleUser.email())
                                    .score("0")
                                    .build()
                    );
                });


        JwtDetails accessJwt = jwtProvider.generateToken(user.getId(), JwtType.ACCESS_TOKEN);
        JwtDetails refreshJwt = jwtProvider.generateToken(user.getId(), JwtType.REFRESH_TOKEN);

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresent(refreshTokenRepository::delete);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(user.getId())
                        .token(refreshJwt.token())
                        .expiryDate(Instant.now().plusMillis(refreshTokenTtl))
                        .build()
        );

        return new TokenResponse(
                accessJwt.token(),
                accessJwt.expiredAt(),
                refreshJwt.token(),
                refreshJwt.expiredAt()
        );
    }
}
