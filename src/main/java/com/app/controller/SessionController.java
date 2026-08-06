package com.app.controller;

import com.app.common.ApiConstants;
import com.app.dto.response.LoginHistoryResponse;
import com.app.dto.response.SessionResponse;
import com.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions & Audit Logging", description = "Multi-device session management, revocation, and login history audit logs")
public class SessionController {

    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all active sessions and devices for the authenticated user")
    public ResponseEntity<List<SessionResponse>> getActiveSessions(Authentication authentication, HttpServletRequest request) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        String authHeader = request.getHeader(ApiConstants.AUTHORIZATION_HEADER);
        String accessToken = authHeader != null && authHeader.startsWith(ApiConstants.BEARER_PREFIX)
            ? authHeader.substring(ApiConstants.BEARER_PREFIX.length()).trim()
            : null;
        return ResponseEntity.ok(authService.getActiveSessions(userPublicId, accessToken));
    }

    @DeleteMapping("/{public_id}")
    @Operation(summary = "Revoke a specific active session by public ID")
    public ResponseEntity<Void> revokeSession(@PathVariable("public_id") UUID sessionPublicId, Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        authService.revokeSession(userPublicId, sessionPublicId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/login-history")
    @Operation(summary = "Retrieve recent login history audit trail for the authenticated user")
    public ResponseEntity<List<LoginHistoryResponse>> getLoginHistory(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.getLoginHistory(userPublicId));
    }
}
