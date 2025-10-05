package com.moon.rabbit.global.security.jwt;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String accessTokenKey;
    private long accessTokenExpires;
    private String refreshTokenKey;
    private long refreshTokenExpires;
}