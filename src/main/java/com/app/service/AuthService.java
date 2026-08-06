package com.app.service;

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

import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request, String ipAddress, String userAgent);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    /**
     * Signs in (or provisions) the user behind a Google ID token. The token is
     * verified against Google's JWKS before any account is touched.
     */
    AuthResponse googleLogin(GoogleLoginRequest request, String ipAddress, String userAgent);

    AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);

    void verifyEmail(VerifyEmailRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userPublicId, ChangePasswordRequest request);

    UserResponse updateProfile(UUID userPublicId, UpdateProfileRequest request);

    void logout(String refreshToken);

    void logoutAllDevices(UUID userPublicId);

    UserResponse getCurrentUser(UUID userPublicId);

    List<SessionResponse> getActiveSessions(UUID userPublicId, String currentRefreshToken);

    void revokeSession(UUID userPublicId, UUID sessionPublicId);

    List<LoginHistoryResponse> getLoginHistory(UUID userPublicId);
}
