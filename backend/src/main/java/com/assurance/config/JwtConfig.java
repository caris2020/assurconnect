package com.assurance.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    public static final String DEFAULT_SECRET = "change-this-super-secret-key-change-this-super-secret-key";
    public static final long DEFAULT_EXPIRATION_MS = 1000L * 60 * 60; // 1h

    @Bean
    public SecretKey jwtSecretKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) secret = DEFAULT_SECRET;
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Will pad/truncate as needed by HMAC-SHA
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Bean
    public Long jwtExpirationMs() {
        String exp = System.getenv("JWT_EXPIRATION_MS");
        if (exp == null || exp.isBlank()) return DEFAULT_EXPIRATION_MS;
        try { return Long.parseLong(exp); } catch (Exception ignored) { return DEFAULT_EXPIRATION_MS; }
    }
}


