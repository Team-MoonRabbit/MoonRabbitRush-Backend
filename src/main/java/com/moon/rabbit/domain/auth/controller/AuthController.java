package com.moon.rabbit.domain.auth.controller;

import com.moon.rabbit.domain.auth.dto.TokenResponse;
import com.moon.rabbit.domain.auth.service.AuthService;
import com.moon.rabbit.domain.auth.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final GoogleOAuthService googleOAuthService;
    private final AuthService authService;

    @GetMapping("/google/url")
    public ResponseEntity<String> googleLoginUrl() {
        String url = googleOAuthService.buildGoogleLoginUrl();
        return ResponseEntity.ok(url);
    }

    @PostMapping("/google/login")
    public ResponseEntity<TokenResponse> googleLogin(@RequestParam String code) {
        TokenResponse response = googleOAuthService.loginWithCode(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Refresh-Token") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok("로그아웃 성공");
    }
}
