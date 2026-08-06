package com.app.controller;

import com.app.common.ApiConstants;
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
import com.app.dto.response.UserResponse;
import com.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity Management", description = "Enterprise Auth, OAuth, Verification, Password Reset & Session Endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new enterprise user account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        String ipAddress = extractIpAddress(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, ipAddress, userAgent));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String ipAddress = extractIpAddress(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ipAddress, userAgent));
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate or register with a Google ID token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sign-in successful"),
        @ApiResponse(responseCode = "401", description = "ID token invalid, expired, or issued to another client")
    })
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpServletRequest) {
        String ipAddress = extractIpAddress(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.googleLogin(request, ipAddress, userAgent));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email with token")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset token")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with reset token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for currently authenticated user")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        UUID publicId = UUID.fromString(authentication.getName());
        authService.changePassword(publicId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile details for currently authenticated user")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        UUID publicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.updateProfile(publicId, request));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Obtain a new access token using a valid refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        String ipAddress = extractIpAddress(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.refreshToken(request, ipAddress, userAgent));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke current refresh token session and logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String token = request != null ? request.refreshToken() : null;
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke all active sessions for current user across all devices")
    public ResponseEntity<Void> logoutAllDevices(Authentication authentication) {
        UUID publicId = UUID.fromString(authentication.getName());
        authService.logoutAllDevices(publicId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve currently authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UUID publicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.getCurrentUser(publicId));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
