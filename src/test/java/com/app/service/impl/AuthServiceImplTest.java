package com.app.service.impl;

import com.app.dto.request.LoginRequest;
import com.app.dto.request.RefreshTokenRequest;
import com.app.dto.request.RegisterRequest;
import com.app.dto.response.AuthResponse;
import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.UnauthorizedException;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import com.app.service.JwtService;
import com.app.util.DateTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .fullName("Jane Doe")
            .email("jane.doe@example.com")
            .passwordHash("encoded-password")
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .build();
    }

    @Test
    void registerHashesPasswordAndReturnsTokens() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_secret");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(UserEntity.class))).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken()).thenReturn("refresh_token_123");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(86400000L);

        AuthResponse response = authService.register(
            new RegisterRequest("  Jane Doe ", " JANE.DOE@EXAMPLE.COM ", "Password123!"), "127.0.0.1", "Mozilla/5.0");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed_secret");
        assertThat(response.accessToken()).isEqualTo("access_token_123");
        assertThat(response.refreshToken()).isEqualTo("refresh_token_123");
        assertThat(response.user().email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void registerThrowsConflictWhenEmailExists() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("Jane Doe", "jane.doe@example.com", "Password123!"), "127.0.0.1", "Mozilla/5.0"))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginAuthenticatesSuccessfully() {
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken()).thenReturn("refresh_token_123");

        AuthResponse response = authService.login(new LoginRequest("jane.doe@example.com", "Password123!"), "127.0.0.1", "Mozilla/5.0");

        assertThat(response.accessToken()).isEqualTo("access_token_123");
        assertThat(response.refreshToken()).isEqualTo("refresh_token_123");
    }

    @Test
    void loginFailsOnInvalidPassword() {
        when(userRepository.findByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane.doe@example.com", "WrongPassword"), "127.0.0.1", "Mozilla/5.0"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Invalid email or password");
    }

    @Test
    void refreshTokenReturnsNewAccessToken() {
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .publicId(UUID.randomUUID())
            .user(testUser)
            .tokenHash("valid_refresh_token")
            .expiresAt(DateTimeUtil.nowUtc().plusDays(1))
            .isRevoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHashAndDeletedAtIsNull("valid_refresh_token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("valid_refresh_token"), "127.0.0.1", "Mozilla/5.0");

        assertThat(response.accessToken()).isEqualTo("new_access_token");
        assertThat(response.refreshToken()).isEqualTo("valid_refresh_token");
    }
}
