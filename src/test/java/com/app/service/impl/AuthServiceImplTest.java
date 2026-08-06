package com.app.service.impl;

import com.app.config.AuthProperties;
import com.app.config.CredentialRateLimiter;
import com.app.dto.request.GoogleLoginRequest;
import com.app.dto.request.LoginRequest;
import com.app.dto.request.RefreshTokenRequest;
import com.app.dto.request.RegisterRequest;
import com.app.dto.response.AuthResponse;
import com.app.entity.OAuthAccountEntity;
import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.UnauthorizedException;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.OAuthAccountRepository;
import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import com.app.service.AuthAuditService;
import com.app.service.GoogleIdentityService;
import com.app.service.JwtService;
import com.app.service.SessionRevocationService;
import com.app.util.DateTimeUtil;
import com.app.util.SecureTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    private static final String IP = "203.0.113.7";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh)";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OAuthAccountRepository oAuthAccountRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthAuditService authAuditService;
    @Mock private GoogleIdentityService googleIdentityService;
    @Mock private SessionRevocationService sessionRevocationService;

    private AuthProperties authProperties;
    private AuthServiceImpl authService;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties(5, 15, 30, 24, 2, 90, 20, 60);
        authService = new AuthServiceImpl(
            userRepository,
            refreshTokenRepository,
            oAuthAccountRepository,
            loginHistoryRepository,
            jwtService,
            passwordEncoder,
            authAuditService,
            googleIdentityService,
            sessionRevocationService,
            new CredentialRateLimiter(authProperties),
            authProperties);

        testUser = UserEntity.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .fullName("Jane Doe")
            .email("jane.doe@example.com")
            .passwordHash("encoded-password")
            .passwordLoginEnabled(true)
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .build();

        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(UserEntity.class), any(UUID.class))).thenReturn("access_token_123");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900_000L);
        when(passwordEncoder.encode(any())).thenReturn("hashed_secret");
    }

    @Test
    void registerNormalisesEmailAndStoresOnlyTheVerificationTokenDigest() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(false);

        AuthResponse response = authService.register(
            new RegisterRequest("  Jane Doe ", " JANE.DOE@EXAMPLE.COM ", "Password123!"), IP, USER_AGENT);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(saved.getFullName()).isEqualTo("Jane Doe");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_secret");
        assertThat(saved.isPasswordLoginEnabled()).isTrue();
        // A SHA-256 hex digest, not the token that was emailed.
        assertThat(saved.getVerificationTokenHash()).hasSize(64).matches("[0-9a-f]+");
        assertThat(response.user().email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void registerThrowsConflictWhenEmailExists() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("Jane Doe", "jane.doe@example.com", "Password123!"), IP, USER_AGENT))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginStoresOnlyTheRefreshTokenDigestAndReturnsThePlaintextOnce() {
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);

        AuthResponse response = authService.login(
            new LoginRequest("jane.doe@example.com", "Password123!"), IP, USER_AGENT);

        ArgumentCaptor<RefreshTokenEntity> sessionCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(sessionCaptor.capture());

        String storedHash = sessionCaptor.getValue().getTokenHash();
        assertThat(storedHash).isNotEqualTo(response.refreshToken());
        assertThat(storedHash).isEqualTo(SecureTokenUtil.hash(response.refreshToken()));
        assertThat(testUser.getLastLoginAt()).isNotNull();
        assertThat(testUser.getFailedLoginAttempts()).isZero();
    }

    @Test
    void loginRecordsFailedAttemptOutOfBandSoTheLockoutSurvivesTheRollback() {
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("jane.doe@example.com", "WrongPassword"), IP, USER_AGENT))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Invalid email or password");

        verify(authAuditService).registerFailedAttempt(1L);
        verify(authAuditService).recordFailure(
            eq(1L), eq("jane.doe@example.com"), eq("Invalid password"), eq(IP), eq(USER_AGENT));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginRejectsLockedAccountWithoutCheckingThePassword() {
        testUser.setLockedUntil(DateTimeUtil.nowUtc().plusMinutes(10));
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("jane.doe@example.com", "Password123!"), IP, USER_AGENT))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("locked");

        verify(passwordEncoder, never()).matches(any(), eq("encoded-password"));
    }

    @Test
    void loginRejectsGoogleOnlyAccount() {
        testUser.setPasswordLoginEnabled(false);
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("jane.doe@example.com", "Password123!"), IP, USER_AGENT))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Google");
    }

    @Test
    void loginOnUnknownEmailStillPerformsAPasswordComparison() {
        when(userRepository.findByEmailAndDeletedAtIsNull("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("ghost@example.com", "Password123!"), IP, USER_AGENT))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Invalid email or password");

        // Same work as a wrong password, so latency does not reveal the account exists.
        verify(passwordEncoder).matches(eq("Password123!"), eq("hashed_secret"));
    }

    @Test
    void refreshRotatesTheSessionAndIssuesANewRefreshToken() {
        String presented = "presented-refresh-token";
        RefreshTokenEntity session = RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(testUser)
            .tokenHash(SecureTokenUtil.hash(presented))
            .expiresAt(DateTimeUtil.nowUtc().plusDays(1))
            .isRevoked(false)
            .build();

        when(refreshTokenRepository.findActiveByTokenHash(SecureTokenUtil.hash(presented)))
            .thenReturn(Optional.of(session));

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest(presented), IP, USER_AGENT);

        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedReason()).isEqualTo("ROTATED");
        assertThat(response.refreshToken()).isNotEqualTo(presented);
        assertThat(response.accessToken()).isEqualTo("access_token_123");
    }

    @Test
    void refreshWithAnAlreadyRotatedTokenRevokesEverySessionOfTheAccount() {
        String presented = "replayed-refresh-token";
        RefreshTokenEntity revoked = RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(testUser)
            .tokenHash(SecureTokenUtil.hash(presented))
            .expiresAt(DateTimeUtil.nowUtc().plusDays(1))
            .isRevoked(true)
            .build();

        when(refreshTokenRepository.findActiveByTokenHash(SecureTokenUtil.hash(presented)))
            .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(presented), IP, USER_AGENT))
            .isInstanceOf(UnauthorizedException.class);

        verify(sessionRevocationService).revokeAllForUser(1L, "REUSE_DETECTED");
    }

    @Test
    void logoutLooksTheSessionUpByDigestAndRevokesIt() {
        String presented = "live-refresh-token";
        RefreshTokenEntity session = RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(testUser)
            .tokenHash(SecureTokenUtil.hash(presented))
            .expiresAt(DateTimeUtil.nowUtc().plusDays(1))
            .isRevoked(false)
            .build();

        when(refreshTokenRepository.findActiveByTokenHash(SecureTokenUtil.hash(presented)))
            .thenReturn(Optional.of(session));

        authService.logout(presented);

        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedReason()).isEqualTo("LOGOUT");
    }

    @Test
    void googleLoginProvisionsAPasswordlessAccountForANewIdentity() {
        when(googleIdentityService.verify("google-id-token"))
            .thenReturn(new GoogleIdentityService.GoogleProfile("google-sub-1", "New User@Example.com", "New User"));
        when(oAuthAccountRepository.findByProviderAndProviderUserIdAndDeletedAtIsNull("GOOGLE", "google-sub-1"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("new user@example.com")).thenReturn(Optional.empty());

        authService.googleLogin(new GoogleLoginRequest("google-id-token"), IP, USER_AGENT);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isPasswordLoginEnabled()).isFalse();
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();

        ArgumentCaptor<OAuthAccountEntity> oauthCaptor = ArgumentCaptor.forClass(OAuthAccountEntity.class);
        verify(oAuthAccountRepository).save(oauthCaptor.capture());
        assertThat(oauthCaptor.getValue().getProviderUserId()).isEqualTo("google-sub-1");
    }

    @Test
    void googleLoginLinksToAnExistingAccountWithTheSameEmail() {
        when(googleIdentityService.verify("google-id-token"))
            .thenReturn(new GoogleIdentityService.GoogleProfile("google-sub-2", "jane.doe@example.com", "Jane Doe"));
        when(oAuthAccountRepository.findByProviderAndProviderUserIdAndDeletedAtIsNull("GOOGLE", "google-sub-2"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));

        authService.googleLogin(new GoogleLoginRequest("google-id-token"), IP, USER_AGENT);

        // The existing account is reused, not duplicated, and keeps password login.
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(oAuthAccountRepository).save(any(OAuthAccountEntity.class));
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.isPasswordLoginEnabled()).isTrue();
    }
}
