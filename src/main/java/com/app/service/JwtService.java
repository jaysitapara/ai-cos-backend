package com.app.service;

import com.app.entity.UserEntity;
import io.jsonwebtoken.Claims;

/** Issues and verifies the stateless bearer tokens used for authentication. */
public interface JwtService {

    String generateAccessToken(UserEntity user);

    String generateRefreshToken();

    boolean isTokenValid(String token);

    String extractSubject(String token);

    Claims extractAllClaims(String token);

    long getAccessTokenExpirationMillis();
}
