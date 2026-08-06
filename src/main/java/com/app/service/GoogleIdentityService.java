package com.app.service;

import com.app.config.GoogleOAuthProperties;
import com.app.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verifies Google Identity Services ID tokens server side.
 *
 * <p>This is the trust boundary for "Sign in with Google": the client sends the
 * ID token Google minted for it and nothing else. The signature is checked
 * against Google's published JWKS, and the issuer, audience and expiry are all
 * validated here, so a caller cannot assert an identity it does not own. No
 * field of the returned profile is ever taken from the request body.
 *
 * <p>The decoder is built lazily and cached. {@link NimbusJwtDecoder} keeps the
 * JWKS in memory and refreshes it on key rotation, so a sustained sign-in rate
 * costs no extra round trips to Google.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleIdentityService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_NAME = "name";

    private final GoogleOAuthProperties properties;

    private volatile JwtDecoder decoder;

    public boolean isEnabled() {
        return properties.isConfigured();
    }

    /**
     * @return the verified Google profile behind {@code idToken}
     * @throws UnauthorizedException if the token is missing, malformed, expired,
     *                               signed by an unknown key, or issued to another client
     */
    public GoogleProfile verify(String idToken) {
        if (!isEnabled()) {
            throw new UnauthorizedException("Google sign-in is not configured on this server");
        }

        Jwt jwt;
        try {
            jwt = resolveDecoder().decode(idToken);
        } catch (JwtException ex) {
            log.warn("Rejected Google ID token: {}", ex.getMessage());
            throw new UnauthorizedException("Google sign-in failed: the identity token is not valid");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new UnauthorizedException("Google sign-in failed: the identity token is missing a subject or email");
        }

        Boolean emailVerified = jwt.getClaimAsBoolean(CLAIM_EMAIL_VERIFIED);
        if (!Boolean.TRUE.equals(emailVerified)) {
            // Without this check an attacker could register an unverified Google
            // address and take over the matching password account by linking.
            throw new UnauthorizedException("Google sign-in failed: the Google account email is not verified");
        }

        String fullName = jwt.getClaimAsString(CLAIM_NAME);
        return new GoogleProfile(subject, email, fullName != null && !fullName.isBlank() ? fullName : email);
    }

    private JwtDecoder resolveDecoder() {
        JwtDecoder current = this.decoder;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (this.decoder == null) {
                NimbusJwtDecoder built = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
                built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    issuerValidator(),
                    audienceValidator()
                ));
                this.decoder = built;
            }
            return this.decoder;
        }
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        List<String> accepted = properties.issuers();
        return jwt -> accepted.contains(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Unexpected ID token issuer", null));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        String clientId = properties.clientId();
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience", "ID token was issued for another client", null));
    }

    /** The subset of the verified Google profile this application consumes. */
    public record GoogleProfile(String subject, String email, String fullName) {
    }
}
