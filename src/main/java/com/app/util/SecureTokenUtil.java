package com.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generation and hashing of opaque bearer secrets (refresh tokens, email
 * verification tokens, password reset tokens).
 *
 * <p>Only the SHA-256 digest of a secret is ever persisted. The plaintext is
 * returned to the caller once, at issue time, and is unrecoverable afterwards,
 * so a database disclosure cannot be replayed against the API. SHA-256 is the
 * correct primitive here rather than Argon2/BCrypt: these are 256-bit random
 * values with no guessable structure, so there is nothing for a slow KDF to
 * defend against, and lookups must stay indexable and O(1).
 */
public final class SecureTokenUtil {

    /** 256 bits of entropy — the same strength as the digest that stores it. */
    private static final int TOKEN_BYTE_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokenUtil() {
        // Private constructor for utility class
    }

    /** Generates a URL-safe, cryptographically random secret to hand to the client. */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** Returns the lowercase hex SHA-256 digest stored in place of the plaintext token. */
    public static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JDK spec; unreachable on any supported runtime.
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
