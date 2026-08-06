package com.app.service.impl;

import com.app.common.ApiConstants;
import com.app.dto.request.ChangePasswordRequest;
import com.app.dto.request.ForgotPasswordRequest;
import com.app.dto.request.LoginRequest;
import com.app.dto.request.OAuthLoginRequest;
import com.app.dto.request.RefreshTokenRequest;
import com.app.dto.request.RegisterRequest;
import com.app.dto.request.ResetPasswordRequest;
import com.app.dto.request.UpdateProfileRequest;
import com.app.dto.request.VerifyEmailRequest;
import com.app.dto.response.AuthResponse;
import com.app.dto.response.LoginHistoryResponse;
import com.app.dto.response.SessionResponse;
import com.app.dto.response.UserResponse;
import com.app.entity.LoginHistoryEntity;
import com.app.entity.OAuthAccountEntity;
import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.UnauthorizedException;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.OAuthAccountRepository;
import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import com.app.service.AuthService;
import com.app.service.JwtService;
import com.app.util.DateTimeUtil;
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

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new DuplicateResourceException("A user with email '%s' already exists".formatted(email));
        }

        String verificationToken = UUID.randomUUID().toString();
        UserEntity user = UserEntity.builder()
            .publicId(UUID.randomUUID())
            .email(email)
            .fullName(request.fullName().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .emailVerified(false)
            .verificationToken(verificationToken)
            .verificationExpiresAt(DateTimeUtil.nowUtc().plusHours(TOKEN_EXPIRATION_HOURS))
            .failedLoginAttempts(0)
            .build();

        UserEntity savedUser = userRepository.save(user);
        recordLoginHistory(savedUser, email, "SUCCESS", null, ipAddress, userAgent);
        log.info("Registered user public_id={}, verificationToken generated", savedUser.getPublicId());

        return createAuthResponse(savedUser, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        OffsetDateTime now = DateTimeUtil.nowUtc();

        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> {
                recordLoginHistory(null, email, "FAILED", "User not found", ipAddress, userAgent);
                return new UnauthorizedException("Invalid email or password");
            });

        if (!user.isAccountNonLocked(now)) {
            recordLoginHistory(user, email, "FAILED", "Account locked", ipAddress, userAgent);
            throw new UnauthorizedException("Account is temporarily locked due to multiple failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(LOCK_TIME_MINUTES));
                log.warn("Account locked for user public_id={}", user.getPublicId());
            }
            userRepository.save(user);
            recordLoginHistory(user, email, "FAILED", "Invalid password", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            recordLoginHistory(user, email, "FAILED", "Account inactive", ipAddress, userAgent);
            throw new UnauthorizedException("User account is not active");
        }

        // Reset failed login count and update last login time
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        recordLoginHistory(user, email, "SUCCESS", null, ipAddress, userAgent);
        log.info("User authenticated public_id={}", user.getPublicId());

        return createAuthResponse(user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        String provider = request.provider().toUpperCase(Locale.ROOT);

        OAuthAccountEntity oauthAccount = oAuthAccountRepository
            .findByProviderAndProviderUserIdAndDeletedAtIsNull(provider, request.providerUserId())
            .orElse(null);

        UserEntity user;

        if (oauthAccount != null) {
            user = oauthAccount.getUser();
        } else {
            user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseGet(() -> {
                UserEntity newUser = UserEntity.builder()
                    .publicId(UUID.randomUUID())
                    .email(email)
                    .fullName(request.fullName().trim())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .failedLoginAttempts(0)
                    .build();
                return userRepository.save(newUser);
            });

            OAuthAccountEntity newOAuth = OAuthAccountEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .provider(provider)
                .providerUserId(request.providerUserId())
                .providerEmail(email)
                .build();
            oAuthAccountRepository.save(newOAuth);
        }

        user.setLastLoginAt(DateTimeUtil.nowUtc());
        userRepository.save(user);

        recordLoginHistory(user, email, "SUCCESS", "OAuth " + provider, ipAddress, userAgent);
        return createAuthResponse(user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(request.refreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UserEntity user = tokenEntity.getUser();
        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        tokenEntity.setLastAccessedAt(DateTimeUtil.nowUtc());
        tokenEntity.setIpAddress(ipAddress);
        tokenEntity.setUserAgent(userAgent);
        refreshTokenRepository.save(tokenEntity);

        String newAccessToken = jwtService.generateAccessToken(user);
        return AuthResponse.of(
            newAccessToken,
            tokenEntity.getTokenHash(),
            jwtService.getAccessTokenExpirationMillis(),
            UserMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        UserEntity user = userRepository.findByVerificationTokenAndDeletedAtIsNull(request.token())
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

        if (user.getVerificationExpiresAt() != null && user.getVerificationExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        userRepository.save(user);
        log.info("Email verified for user public_id={}", user.getPublicId());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            user.setPasswordResetToken(UUID.randomUUID().toString());
            user.setPasswordResetExpiresAt(DateTimeUtil.nowUtc().plusHours(2));
            userRepository.save(user);
            log.info("Password reset token created for user public_id={}", user.getPublicId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByPasswordResetTokenAndDeletedAtIsNull(request.token())
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));

        if (user.getPasswordResetExpiresAt() != null && user.getPasswordResetExpiresAt().isBefore(DateTimeUtil.nowUtc())) {
            throw new UnauthorizedException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Revoke existing sessions on password reset
        revokeAllUserSessions(user);
        log.info("Password reset successful for user public_id={}", user.getPublicId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userPublicId, ChangePasswordRequest request) {
        UserEntity user = findActiveUser(userPublicId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user public_id={}", userPublicId);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userPublicId, UpdateProfileRequest request) {
        UserEntity user = findActiveUser(userPublicId);
        user.setFullName(request.fullName().trim());
        UserEntity saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setUpdatedBy(ApiConstants.SYSTEM_ACTOR);
                    refreshTokenRepository.save(token);
                });
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userPublicId) {
        UserEntity user = findActiveUser(userPublicId);
        revokeAllUserSessions(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userPublicId) {
        return UserMapper.toResponse(findActiveUser(userPublicId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(UUID userPublicId, String currentRefreshToken) {
        UserEntity user = findActiveUser(userPublicId);
        return refreshTokenRepository.findAllByUserAndIsRevokedFalseAndDeletedAtIsNullOrderByLastAccessedAtDesc(user)
            .stream()
            .map(session -> new SessionResponse(
                session.getPublicId(),
                session.getDeviceName() != null ? session.getDeviceName() : "Unknown Device",
                session.getDeviceType() != null ? session.getDeviceType() : "Desktop",
                session.getIpAddress() != null ? session.getIpAddress() : "127.0.0.1",
                session.getUserAgent() != null ? session.getUserAgent() : "Browser",
                session.getLocation() != null ? session.getLocation() : "Unknown",
                session.getLastAccessedAt(),
                session.getExpiresAt(),
                currentRefreshToken != null && currentRefreshToken.equals(session.getTokenHash())
            ))
            .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userPublicId, UUID sessionPublicId) {
        RefreshTokenEntity session = refreshTokenRepository.findByPublicIdAndDeletedAtIsNull(sessionPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getUser().getPublicId().equals(userPublicId)) {
            throw new UnauthorizedException("Cannot revoke session belonging to another user");
        }

        session.setRevoked(true);
        refreshTokenRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getLoginHistory(UUID userPublicId) {
        UserEntity user = findActiveUser(userPublicId);
        return loginHistoryRepository.findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, 20))
            .stream()
            .map(log -> new LoginHistoryResponse(
                log.getPublicId(),
                log.getEmail(),
                log.getStatus(),
                log.getFailureReason(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
            ))
            .toList();
    }

    private AuthResponse createAuthResponse(UserEntity user, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .tokenHash(refreshTokenValue)
            .deviceName(parseDeviceName(userAgent))
            .deviceType(parseDeviceType(userAgent))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .location("Local Network")
            .expiresAt(DateTimeUtil.nowUtc().plusDays(REFRESH_TOKEN_EXPIRATION_DAYS))
            .lastAccessedAt(DateTimeUtil.nowUtc())
            .isRevoked(false)
            .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.of(
            accessToken,
            refreshTokenValue,
            jwtService.getAccessTokenExpirationMillis(),
            UserMapper.toResponse(user)
        );
    }

    private void recordLoginHistory(UserEntity user, String email, String status, String failureReason, String ipAddress, String userAgent) {
        LoginHistoryEntity history = LoginHistoryEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .email(email)
            .status(status)
            .failureReason(failureReason)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        loginHistoryRepository.save(history);
    }

    private void revokeAllUserSessions(UserEntity user) {
        List<RefreshTokenEntity> activeSessions = refreshTokenRepository.findAllByUser(user);
        activeSessions.forEach(session -> session.setRevoked(true));
        refreshTokenRepository.saveAll(activeSessions);
    }

    private UserEntity findActiveUser(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("User with public_id '%s' was not found".formatted(publicId)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
