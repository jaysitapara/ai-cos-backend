package com.app.service.impl;

import com.app.common.ApiConstants;
import com.app.config.AuthProperties;
import com.app.config.CredentialRateLimiter;
import com.app.dto.request.ChangePasswordRequest;
import com.app.dto.request.ForgotPasswordRequest;
import com.app.dto.request.GoogleLoginRequest;
import com.app.dto.request.LoginRequest;
import com.app.dto.request.RefreshTokenRequest;
import com.app.dto.request.RegisterRequest;
import com.app.dto.request.ResetPasswordRequest;
import com.app.dto.request.UpdateProfileRequest;
import com.app.dto.request.VerifyEmailRequest;
import com.app.dto.response.AuthResponse;
import com.app.dto.response.LoginHistoryResponse;
import com.app.dto.response.SessionResponse;
import com.app.dto.response.UserResponse;
import com.app.entity.OAuthAccountEntity;
import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.TooManyRequestsException;
import com.app.exception.UnauthorizedException;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.OAuthAccountRepository;
import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import com.app.service.AuthAuditService;
import com.app.service.AuthService;
import com.app.service.GoogleIdentityService;
import com.app.service.JwtService;
import com.app.service.SessionRevocationService;
import com.app.util.DateTimeUtil;
import com.app.util.SecureTokenUtil;
import com.app.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final int LOGIN_HISTORY_PAGE_SIZE = 20;

    private static final String REVOKED_LOGOUT = "LOGOUT";
    private static final String REVOKED_LOGOUT_ALL = "LOGOUT_ALL";
    private static final String REVOKED_ROTATED = "ROTATED";
    private static final String REVOKED_REUSE_DETECTED = "REUSE_DETECTED";
    private static final String REVOKED_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String REVOKED_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String REVOKED_BY_USER = "REVOKED_BY_USER";

    /** user_agent is a 500 character column and browsers happily send more. */
    private static final int USER_AGENT_MAX_LENGTH = 500;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final GoogleIdentityService googleIdentityService;
    private final SessionRevocationService sessionRevocationService;
    private final CredentialRateLimiter credentialRateLimiter;
    private final AuthProperties authProperties;

    /** @see #dummyPasswordHash() */
    private volatile String dummyPasswordHash;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        requireWithinRateLimit(ipAddress);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new DuplicateResourceException("A user with email '%s' already exists".formatted(email));
        }

        String verificationToken = SecureTokenUtil.generateToken();
        UserEntity user = UserEntity.builder()
            .publicId(UUID.randomUUID())
            .email(email)
            .fullName(request.fullName().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .passwordLoginEnabled(true)
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .emailVerified(false)
            .verificationTokenHash(SecureTokenUtil.hash(verificationToken))
            .verificationExpiresAt(DateTimeUtil.nowUtc().plusHours(authProperties.verificationTokenHours()))
            .failedLoginAttempts(0)
            .build();

        UserEntity savedUser = userRepository.save(user);

        // The plaintext token exists only here; hand it to the mail transport.
        log.debug("Issued email verification token for user public_id={}: {}", savedUser.getPublicId(), verificationToken);
        log.info("Registered user public_id={}", savedUser.getPublicId());

        authAuditService.recordSuccess(savedUser, email, "Registration", ipAddress, userAgent);
        return issueSession(savedUser, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        requireWithinRateLimit(ipAddress);

        String email = normalizeEmail(request.email());
        OffsetDateTime now = DateTimeUtil.nowUtc();

        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash());
            authAuditService.recordFailure(null, email, "User not found", ipAddress, userAgent);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!user.isAccountNonLocked(now)) {
            authAuditService.recordFailure(user.getId(), email, "Account locked", ipAddress, userAgent);
            throw new UnauthorizedException(
                "Account is temporarily locked due to multiple failed attempts. Try again later.");
        }

        // Accounts provisioned through Google hold a random placeholder hash that
        // nobody knows; reject them here rather than letting the comparison decide.
        if (!user.isPasswordLoginEnabled()) {
            authAuditService.recordFailure(user.getId(), email, "Password login disabled", ipAddress, userAgent);
            throw new UnauthorizedException(
                "This account signs in with Google. Use \"Continue with Google\" instead.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Committed in its own transaction — this one is about to roll back.
            authAuditService.registerFailedAttempt(user.getId());
            authAuditService.recordFailure(user.getId(), email, "Invalid password", ipAddress, userAgent);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            authAuditService.recordFailure(user.getId(), email, "Account inactive", ipAddress, userAgent);
            throw new UnauthorizedException("User account is not active");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);

        credentialRateLimiter.reset(ipAddress);
        authAuditService.recordSuccess(user, email, null, ipAddress, userAgent);
        log.info("User authenticated public_id={}", user.getPublicId());

        return issueSession(user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request, String ipAddress, String userAgent) {
        requireWithinRateLimit(ipAddress);

        // Everything below is derived from the verified token, never from the request body.
        GoogleIdentityService.GoogleProfile profile = googleIdentityService.verify(request.idToken());
        String email = normalizeEmail(profile.email());

        UserEntity user = oAuthAccountRepository
            .findByProviderAndProviderUserIdAndDeletedAtIsNull(PROVIDER_GOOGLE, profile.subject())
            .map(OAuthAccountEntity::getUser)
            .orElseGet(() -> linkOrProvisionGoogleUser(profile, email));

        if (user.getStatus() != UserStatus.ACTIVE || user.isDeleted()) {
            authAuditService.recordFailure(user.getId(), email, "Account inactive", ipAddress, userAgent);
            throw new UnauthorizedException("User account is not active");
        }

        user.setLastLoginAt(DateTimeUtil.nowUtc());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        credentialRateLimiter.reset(ipAddress);
        authAuditService.recordSuccess(user, email, "Google sign-in", ipAddress, userAgent);
        log.info("Google sign-in for user public_id={}", user.getPublicId());

        return issueSession(user, ipAddress, userAgent);
    }

    /**
     * Attaches the Google identity to the matching local account, or creates one.
     *
     * <p>Linking by email is only safe because {@link GoogleIdentityService} has
     * already refused any token whose {@code email_verified} claim is not true.
     */
    private UserEntity linkOrProvisionGoogleUser(GoogleIdentityService.GoogleProfile profile, String email) {
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .publicId(UUID.randomUUID())
                .email(email)
                .fullName(profile.fullName().trim())
                // No password was ever chosen: store an unguessable placeholder and
                // keep the password grant switched off for this account.
                .passwordHash(passwordEncoder.encode(SecureTokenUtil.generateToken()))
                .passwordLoginEnabled(false)
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build()));

        // Google has vouched for this address, so an unverified local account becomes verified.
        user.setEmailVerified(true);

        oAuthAccountRepository.save(OAuthAccountEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .provider(PROVIDER_GOOGLE)
            .providerUserId(profile.subject())
            .providerEmail(email)
            .build());

        return user;
    }

    /**
     * Rotates the presented refresh token: the old session is revoked and a fresh
     * pair is issued, so a stolen token is usable at most once before the rightful
     * client's next refresh invalidates it.
     */
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String presentedHash = SecureTokenUtil.hash(request.refreshToken());

        RefreshTokenEntity session = refreshTokenRepository.findActiveByTokenHash(presentedHash)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        UserEntity user = session.getUser();

        if (session.isRevoked()) {
            // A revoked token being presented means either a replay of a rotated
            // token or a stolen one. Both point at a compromised client, so every
            // session for the account is torn down.
            log.warn("Refresh token reuse detected for user public_id={}; revoking all sessions", user.getPublicId());
            // Committed out of band: this transaction is about to roll back.
            sessionRevocationService.revokeAllForUser(user.getId(), REVOKED_REUSE_DETECTED);
            authAuditService.recordFailure(user.getId(), user.getEmail(), "Refresh token reuse detected", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (session.getExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        revoke(session, REVOKED_ROTATED);
        return issueSession(user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        UserEntity user = userRepository
            .findByVerificationTokenHashAndDeletedAtIsNull(SecureTokenUtil.hash(request.token()))
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

        if (user.getVerificationExpiresAt() != null && user.getVerificationExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationTokenHash(null);
        user.setVerificationExpiresAt(null);
        log.info("Email verified for user public_id={}", user.getPublicId());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());

        // Always returns 200 regardless of whether the address exists, so the
        // endpoint cannot be used to test which emails are registered.
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String resetToken = SecureTokenUtil.generateToken();
            user.setPasswordResetTokenHash(SecureTokenUtil.hash(resetToken));
            user.setPasswordResetExpiresAt(
                DateTimeUtil.nowUtc().plusHours(authProperties.passwordResetTokenHours()));

            // The plaintext token exists only here; hand it to the mail transport.
            log.debug("Issued password reset token for user public_id={}: {}", user.getPublicId(), resetToken);
            log.info("Password reset token created for user public_id={}", user.getPublicId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository
            .findByPasswordResetTokenHashAndDeletedAtIsNull(SecureTokenUtil.hash(request.token()))
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));

        if (user.getPasswordResetExpiresAt() != null && user.getPasswordResetExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordLoginEnabled(true);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        // Whoever held the old password must not keep a live session.
        refreshTokenRepository.revokeAllForUser(user.getId(), DateTimeUtil.nowUtc(), REVOKED_PASSWORD_RESET);
        log.info("Password reset successful for user public_id={}", user.getPublicId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userPublicId, ChangePasswordRequest request) {
        UserEntity user = findActiveUser(userPublicId);

        if (user.isPasswordLoginEnabled() && !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordLoginEnabled(true);

        refreshTokenRepository.revokeAllForUser(user.getId(), DateTimeUtil.nowUtc(), REVOKED_PASSWORD_CHANGED);
        log.info("Password changed for user public_id={}", userPublicId);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userPublicId, UpdateProfileRequest request) {
        UserEntity user = findActiveUser(userPublicId);
        user.setFullName(request.fullName().trim());
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findActiveByTokenHash(SecureTokenUtil.hash(refreshToken))
            .ifPresent(session -> revoke(session, REVOKED_LOGOUT));
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userPublicId) {
        UserEntity user = findActiveUser(userPublicId);
        int revoked = refreshTokenRepository.revokeAllForUser(user.getId(), DateTimeUtil.nowUtc(), REVOKED_LOGOUT_ALL);
        log.info("Revoked {} sessions for user public_id={}", revoked, userPublicId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userPublicId) {
        return UserMapper.toResponse(findActiveUser(userPublicId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(UUID userPublicId, String currentAccessToken) {
        UserEntity user = findActiveUser(userPublicId);

        // The caller's own session is identified by the sid claim its access token
        // carries. Comparing the access token against stored refresh digests, as
        // this once did, can never match and marked every device "not current".
        UUID currentSessionId = jwtService.extractSessionId(currentAccessToken);

        return refreshTokenRepository
            .findAllByUserAndIsRevokedFalseAndDeletedAtIsNullOrderByLastAccessedAtDesc(user)
            .stream()
            .map(session -> new SessionResponse(
                session.getPublicId(),
                defaultIfNull(session.getDeviceName(), "Unknown Device"),
                defaultIfNull(session.getDeviceType(), "Desktop"),
                defaultIfNull(session.getIpAddress(), "Unknown"),
                defaultIfNull(session.getUserAgent(), "Browser"),
                defaultIfNull(session.getLocation(), "Unknown"),
                session.getLastAccessedAt(),
                session.getExpiresAt(),
                session.getPublicId().equals(currentSessionId)
            ))
            .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userPublicId, UUID sessionPublicId) {
        RefreshTokenEntity session = refreshTokenRepository.findByPublicIdAndDeletedAtIsNull(sessionPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getUser().getPublicId().equals(userPublicId)) {
            // Reported as "not found" so the endpoint does not confirm that a
            // session id belonging to somebody else exists.
            throw new ResourceNotFoundException("Session not found");
        }

        revoke(session, REVOKED_BY_USER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getLoginHistory(UUID userPublicId) {
        UserEntity user = findActiveUser(userPublicId);
        return loginHistoryRepository
            .findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, LOGIN_HISTORY_PAGE_SIZE))
            .stream()
            .map(entry -> new LoginHistoryResponse(
                entry.getPublicId(),
                entry.getEmail(),
                entry.getStatus(),
                entry.getFailureReason(),
                entry.getIpAddress(),
                entry.getUserAgent(),
                entry.getCreatedAt()
            ))
            .toList();
    }

    /**
     * Creates a persisted session and returns the token pair. Only the SHA-256
     * digest of the refresh token is stored; the plaintext in the response is the
     * only copy that ever leaves this method.
     */
    private AuthResponse issueSession(UserEntity user, String ipAddress, String userAgent) {
        String refreshTokenValue = SecureTokenUtil.generateToken();
        OffsetDateTime now = DateTimeUtil.nowUtc();

        RefreshTokenEntity session = refreshTokenRepository.save(RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .tokenHash(SecureTokenUtil.hash(refreshTokenValue))
            .deviceName(parseDeviceName(userAgent))
            .deviceType(parseDeviceType(userAgent))
            .ipAddress(ipAddress)
            .userAgent(truncateUserAgent(userAgent))
            .expiresAt(now.plusDays(authProperties.refreshTokenValidityDays()))
            .lastAccessedAt(now)
            .isRevoked(false)
            .build());

        String accessToken = jwtService.generateAccessToken(user, session.getPublicId());

        return AuthResponse.of(
            accessToken,
            refreshTokenValue,
            jwtService.getAccessTokenExpirationMillis(),
            UserMapper.toResponse(user)
        );
    }

    private void revoke(RefreshTokenEntity session, String reason) {
        session.setRevoked(true);
        session.setRevokedAt(DateTimeUtil.nowUtc());
        session.setRevokedReason(reason);
        session.setUpdatedBy(ApiConstants.SYSTEM_ACTOR);
    }

    private void requireWithinRateLimit(String ipAddress) {
        if (!credentialRateLimiter.tryAcquire(ipAddress)) {
            log.warn("Rate limit exceeded for credential endpoint from ip={}", ipAddress);
            throw new TooManyRequestsException("Too many attempts. Please wait a moment and try again.");
        }
    }

    private UserEntity findActiveUser(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with public_id '%s' was not found".formatted(publicId)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * A well-formed Argon2 hash of a value nobody knows, compared against when the
     * email does not exist so that an unknown account costs the same wall-clock
     * time as a wrong password. Response latency alone would otherwise enumerate
     * which addresses are registered.
     *
     * <p>Derived from the live {@link PasswordEncoder} rather than hard-coded, so
     * it always matches the encoder's current cost parameters — a literal from a
     * different configuration would be rejected as malformed and skip the work
     * this exists to perform.
     */
    private String dummyPasswordHash() {
        String current = this.dummyPasswordHash;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (this.dummyPasswordHash == null) {
                this.dummyPasswordHash = passwordEncoder.encode(SecureTokenUtil.generateToken());
            }
            return this.dummyPasswordHash;
        }
    }

    private static String truncateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= USER_AGENT_MAX_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, USER_AGENT_MAX_LENGTH);
    }

    private static String defaultIfNull(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null) return "Unknown Device";
        if (userAgent.contains("Macintosh")) return "MacBook / macOS";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("iPhone")) return "iPhone";
        if (userAgent.contains("Android")) return "Android Phone";
        if (userAgent.contains("Linux")) return "Linux Device";
        return "Browser Client";
    }

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) return "Desktop";
        if (userAgent.contains("Mobile") || userAgent.contains("iPhone") || userAgent.contains("Android")) return "Mobile";
        return "Desktop";
    }
}
