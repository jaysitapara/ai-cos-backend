package com.app.service.impl;

import com.app.config.JwtProperties;
import com.app.entity.UserEntity;
import com.app.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtServiceImpl(JwtProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < JwtProperties.MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least %d characters long".formatted(JwtProperties.MIN_SECRET_LENGTH));
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = jwtProperties.expiration();
    }

    @Override
    public String generateAccessToken(UserEntity user, UUID sessionPublicId) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
            .subject(user.getPublicId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim(CLAIM_SESSION_ID, sessionPublicId.toString())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusMillis(expirationMillis)))
            .signWith(signingKey)
            .compact();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public Claims extractAllClaims(String token) {
        return parseClaims(token);
    }

    @Override
    public UUID extractSessionId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String sessionId = parseClaims(token).get(CLAIM_SESSION_ID, String.class);
            return sessionId != null ? UUID.fromString(sessionId) : null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Unable to read session id from token: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public long getAccessTokenExpirationMillis() {
        return expirationMillis;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
