package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.dto.response.LoginHistoryResponse;
import com.app.dto.response.SessionResponse;
import com.app.entity.UserEntity;
import com.app.exception.UnauthorizedException;
import com.app.repository.UserRepository;
import com.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/sessions")
@RequiredArgsConstructor
@Tag(name = "V1 User Sessions", description = "Active Sessions & Login History REST APIs")
public class V1SessionController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get active user sessions")
    public ResponseEntity<List<SessionResponse>> getActiveSessions(Authentication authentication) {
        UUID userPublicId = getUserPublicId(authentication);
        return ResponseEntity.ok(authService.getActiveSessions(userPublicId, null));
    }

    @DeleteMapping("/{sessionPublicId}")
    @Operation(summary = "Revoke active session by session public ID")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionPublicId, Authentication authentication) {
        UUID userPublicId = getUserPublicId(authentication);
        authService.revokeSession(userPublicId, sessionPublicId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/login-history")
    @Operation(summary = "Get user login attempt history")
    public ResponseEntity<List<LoginHistoryResponse>> getLoginHistory(Authentication authentication) {
        UUID userPublicId = getUserPublicId(authentication);
        return ResponseEntity.ok(authService.getLoginHistory(userPublicId));
    }

    private UUID getUserPublicId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User authentication required");
        }
        String name = authentication.getName();
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
            return user.getPublicId();
        }
    }
}
