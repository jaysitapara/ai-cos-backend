package com.app.service;

import com.app.entity.UserEntity;
import io.jsonwebtoken.Claims;

import java.util.UUID;

/** Issues and verifies the stateless bearer tokens used for authentication. */
public interface JwtService {

    /** Claim carrying the public id of the refresh-token session that minted this access token. */
    String CLAIM_SESSION_ID = "sid";

    /**
     * @param sessionPublicId the session this token belongs to, embedded as {@code sid}
     *                        so a request can tell which of the user's devices it came from
     */
    String generateAccessToken(UserEntity user, UUID sessionPublicId);

    boolean isTokenValid(String token);

    String extractSubject(String token);

    Claims extractAllClaims(String token);

    /** @return the {@code sid} claim, or {@code null} if the token is absent or unreadable */
    UUID extractSessionId(String token);

    long getAccessTokenExpirationMillis();
}
