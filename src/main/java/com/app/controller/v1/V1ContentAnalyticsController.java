package com.app.controller.v1;

import com.app.entity.ContentAnalyticsEntity;
import com.app.entity.ContentIntelligenceInsightEntity;
import com.app.entity.ContentPerformanceSnapshotEntity;
import com.app.entity.UserEntity;
import com.app.exception.UnauthorizedException;
import com.app.repository.UserRepository;
import com.app.service.ContentAnalyticsService;
import com.app.service.ContentIntelligenceEngine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class V1ContentAnalyticsController {

    private final ContentAnalyticsService analyticsService;
    private final ContentIntelligenceEngine intelligenceEngine;
    private final UserRepository userRepository;

    @GetMapping("/brands/{brandPublicId}/analytics")
    public ResponseEntity<Map<String, Object>> getBrandAnalyticsOverview(
            Authentication authentication,
            @PathVariable UUID brandPublicId
    ) {
        UserEntity user = getAuthenticatedUser(authentication);
        Map<String, Object> overview = analyticsService.getBrandAnalyticsOverview(user, brandPublicId);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/brands/{brandPublicId}/analytics/content")
    public ResponseEntity<List<ContentAnalyticsEntity>> getBrandContentAnalyticsList(
            Authentication authentication,
            @PathVariable UUID brandPublicId
    ) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<ContentAnalyticsEntity> list = analyticsService.getBrandContentAnalyticsList(user, brandPublicId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/brands/{brandPublicId}/analytics/insights")
    public ResponseEntity<List<ContentIntelligenceInsightEntity>> getBrandInsights(
            Authentication authentication,
            @PathVariable UUID brandPublicId
    ) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<ContentIntelligenceInsightEntity> insights = intelligenceEngine.getBrandInsights(user, brandPublicId);
        return ResponseEntity.ok(insights);
    }

    @PostMapping("/brands/{brandPublicId}/intelligence/refresh")
    public ResponseEntity<List<ContentIntelligenceInsightEntity>> refreshBrandIntelligence(
            Authentication authentication,
            @PathVariable UUID brandPublicId
    ) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<ContentIntelligenceInsightEntity> refreshed = intelligenceEngine.refreshBrandIntelligence(user, brandPublicId);
        return ResponseEntity.ok(refreshed);
    }

    @Data
    public static class PerformanceSnapshotRequest {
        private String platform;
        private Long impressions;
        private Long reach;
        private Long likes;
        private Long comments;
        private Long shares;
        private Long saves;
        private Long clicks;
        private Long conversions;
    }

    @PostMapping("/content/analytics/{analyticsPublicId}/performance")
    public ResponseEntity<ContentPerformanceSnapshotEntity> recordPerformanceSnapshot(
            Authentication authentication,
            @PathVariable UUID analyticsPublicId,
            @RequestBody PerformanceSnapshotRequest req
    ) {
        UserEntity user = getAuthenticatedUser(authentication);
        ContentPerformanceSnapshotEntity snapshot = analyticsService.recordPerformanceSnapshot(
            user, analyticsPublicId, req.getPlatform(), req.getImpressions(), req.getReach(),
            req.getLikes(), req.getComments(), req.getShares(), req.getSaves(),
            req.getClicks(), req.getConversions()
        );
        return ResponseEntity.ok(snapshot);
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User authentication required");
        }
        String name = authentication.getName();
        try {
            UUID publicId = UUID.fromString(name);
            return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                    .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(name)
                            .orElseThrow(() -> new UnauthorizedException("Authenticated user not found")));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        }
    }
}
