package com.example.Auto_Grade.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String accessSecret;
    private String refreshSecret;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;
}
