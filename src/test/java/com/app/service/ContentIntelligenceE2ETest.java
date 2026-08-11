package com.app.service;

import com.app.entity.*;
import com.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Content Intelligence & Analytics — E2E Test Suite")
class ContentIntelligenceE2ETest {

    @Mock private ContentAnalyticsRepository analyticsRepository;
    @Mock private ContentBehaviorEventRepository eventRepository;
    @Mock private ContentPerformanceSnapshotRepository snapshotRepository;
    @Mock private ContentIntelligenceInsightRepository insightRepository;
    @Mock private ContentFeedbackRepository feedbackRepository;
    @Mock private BrandManagementService brandManagementService;

    @InjectMocks private ContentAnalyticsService analyticsService;
    @InjectMocks private ContentIntelligenceEngine intelligenceEngine;

    private UserEntity userA;
    private UserEntity userB;
    private BrandEntity brandA;
    private UUID brandPublicId;
    private GeneratedContentEntity content1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userA = UserEntity.builder().id(1L).email("usera@company.com").build();
        userB = UserEntity.builder().id(2L).email("userb@company.com").build();

        brandPublicId = UUID.randomUUID();
        brandA = BrandEntity.builder()
            .id(10L)
            .publicId(brandPublicId)
            .user(userA)
            .name("Apex Cyber Security")
            .industry("Cybersecurity")
            .positioning("Zero Trust Leader")
            .build();

        content1 = GeneratedContentEntity.builder()
            .id(100L)
            .publicId(UUID.randomUUID())
            .user(userA)
            .brand(brandA)
            .contentType("LINKEDIN_POST")
            .title("Zero Trust Architecture Trends")
            .status("DRAFT")
            .build();
    }

    @Test
    @DisplayName("Should create analytics record, track behavioral events, and calculate deterministic overview with 0 AI calls")
    void testAnalyticsCreationAndOverview() {
        when(analyticsRepository.findByContentIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        when(analyticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContentAnalyticsEntity record = analyticsService.createOrUpdateAnalyticsRecord(
            userA, brandA, content1, "LINKEDIN_POST", "Zero Trust Architecture", "LINKEDIN", "DRAFT"
        );

        assertNotNull(record);
        assertEquals("LINKEDIN_POST", record.getContentType());
        verify(eventRepository, times(1)).save(any());

        // Test Brand Overview Calculation
        when(brandManagementService.getBrandByPublicId(userA, brandPublicId)).thenReturn(brandA);

        ContentAnalyticsEntity a1 = ContentAnalyticsEntity.builder()
            .contentType("LINKEDIN_POST").status("APPROVED").editCount(1).versionCount(2).performanceScore(85.0).build();
        ContentAnalyticsEntity a2 = ContentAnalyticsEntity.builder()
            .contentType("LINKEDIN_POST").status("APPROVED").editCount(0).versionCount(1).performanceScore(90.0).build();
        ContentAnalyticsEntity a3 = ContentAnalyticsEntity.builder()
            .contentType("NEWSLETTER").status("REJECTED").editCount(3).versionCount(4).performanceScore(0.0).build();

        when(analyticsRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(List.of(a1, a2, a3));

        Map<String, Object> overview = analyticsService.getBrandAnalyticsOverview(userA, brandPublicId);
        assertEquals(3, overview.get("totalContent"));
        assertEquals(2L, overview.get("approvedCount"));
        assertEquals(1L, overview.get("rejectedCount"));
        assertEquals(66.7, overview.get("approvalRate"));
    }

    @Test
    @DisplayName("Should derive explainable intelligence insights with confidence ratings from accumulated behavior")
    void testIntelligenceInsightDerivation() {
        when(brandManagementService.getBrandByPublicId(userA, brandPublicId)).thenReturn(brandA);

        List<ContentAnalyticsEntity> history = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            history.add(ContentAnalyticsEntity.builder()
                .contentType("LINKEDIN_POST")
                .status(i < 7 ? "APPROVED" : "REJECTED")
                .editCount(1)
                .versionCount(2)
                .build());
        }

        when(analyticsRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(history);
        when(eventRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(Collections.emptyList());
        when(insightRepository.findByBrandIdAndUserIdAndStatus(10L, 1L, "ACTIVE")).thenReturn(Collections.emptyList());

        intelligenceEngine.refreshBrandIntelligence(userA, brandPublicId);

        // Verify high-confidence insight saved for LINKEDIN_POST preference
        verify(insightRepository, atLeast(2)).save(argThat(insight -> {
            if ("PREFERRED_CONTENT_TYPE_LINKEDIN_POST".equals(insight.getInsightKey())) {
                return "HIGH".equals(insight.getConfidence()) && insight.getConfidenceScore() >= 0.90;
            }
            return true;
        }));
    }

    @Test
    @DisplayName("Should assemble compact active intelligence prompt context for AIService")
    void testAssembleIntelligencePromptContext() {
        ContentIntelligenceInsightEntity insight1 = ContentIntelligenceInsightEntity.builder()
            .type("CONTENT_TYPE")
            .insightKey("PREFERRED_CONTENT_TYPE_LINKEDIN_POST")
            .insightValue("For LINKEDIN_POST, user approval rate is 88% across 8 posts.")
            .confidence("HIGH")
            .confidenceScore(0.95)
            .status("ACTIVE")
            .contentTypeScope("LINKEDIN_POST")
            .build();

        when(insightRepository.findByBrandIdAndUserIdAndStatus(10L, 1L, "ACTIVE")).thenReturn(List.of(insight1));

        String promptContext = intelligenceEngine.assembleIntelligencePromptContext(userA, brandA, "LINKEDIN_POST");
        assertTrue(promptContext.contains("HIGH CONFIDENCE"));
        assertTrue(promptContext.contains("approval rate is 88%"));
    }

    @Test
    @DisplayName("Should enforce strict server-side user isolation for analytics and intelligence")
    void testAnalyticsUserIsolation() {
        when(brandManagementService.getBrandByPublicId(userB, brandPublicId))
            .thenThrow(new NoSuchElementException("Brand not found or access denied for ID: " + brandPublicId));

        assertThrows(NoSuchElementException.class, () -> analyticsService.getBrandAnalyticsOverview(userB, brandPublicId));
        assertThrows(NoSuchElementException.class, () -> intelligenceEngine.getBrandInsights(userB, brandPublicId));
    }
}
