package com.app.service;

import com.app.entity.*;
import com.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentIntelligenceEngine {

    private final ContentIntelligenceInsightRepository insightRepository;
    private final ContentAnalyticsRepository analyticsRepository;
    private final ContentBehaviorEventRepository eventRepository;
    private final ContentFeedbackRepository feedbackRepository;
    private final BrandManagementService brandManagementService;

    @Transactional
    public List<ContentIntelligenceInsightEntity> refreshBrandIntelligence(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        List<ContentAnalyticsEntity> analyticsList = analyticsRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
        List<ContentBehaviorEventEntity> events = eventRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());

        List<ContentIntelligenceInsightEntity> insights = new ArrayList<>();

        if (analyticsList.isEmpty()) {
            log.info("No content analytics found for brand [{}] - skipping insight derivation.", brand.getName());
            return insights;
        }

        // 1. Content Type Approval Insight
        Map<String, List<ContentAnalyticsEntity>> byType = analyticsList.stream()
            .collect(Collectors.groupingBy(ContentAnalyticsEntity::getContentType));

        byType.forEach((type, list) -> {
            long approved = list.stream().filter(a -> "APPROVED".equalsIgnoreCase(a.getStatus()) || "PUBLISHED".equalsIgnoreCase(a.getStatus())).count();
            int total = list.size();
            double rate = (double) approved / total;

            String key = "PREFERRED_CONTENT_TYPE_" + type;
            String confidence = total >= 7 ? "HIGH" : (total >= 3 ? "MEDIUM" : "LOW");
            double confScore = total >= 7 ? 0.95 : (total >= 3 ? 0.75 : 0.4);

            String val = "For " + type + ", user approval rate is " + Math.round(rate * 100) + "% across " + total + " posts.";
            String summary = "Based on " + approved + " approved items out of " + total + " total generated " + type + " drafts.";

            saveOrUpdateInsight(user, brand, "CONTENT_TYPE", key, val, confidence, confScore, summary, total, "BRAND", type);
        });

        // 2. Average Edits & Length Preference Insight
        double avgEdits = analyticsList.stream().mapToInt(ContentAnalyticsEntity::getEditCount).average().orElse(0.0);
        int evidenceCount = analyticsList.size();
        String lengthConf = evidenceCount >= 7 ? "HIGH" : (evidenceCount >= 3 ? "MEDIUM" : "LOW");
        double lengthConfScore = evidenceCount >= 7 ? 0.90 : (evidenceCount >= 3 ? 0.70 : 0.35);

        String lengthVal = avgEdits < 2.0
            ? "User generally approves AI drafts with minimal manual edits (avg " + String.format("%.1f", avgEdits) + " edits/post)."
            : "User prefers making iterative polish edits (avg " + String.format("%.1f", avgEdits) + " edits/post) before final approval.";
        String lengthSummary = "Derived from edit history across " + evidenceCount + " generated posts.";

        saveOrUpdateInsight(user, brand, "PREFERENCE", "EDIT_BEHAVIOR", lengthVal, lengthConf, lengthConfScore, lengthSummary, evidenceCount, "BRAND", null);

        // 3. Rejection / Aversion Insight
        long rejectedCount = analyticsList.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getStatus())).count();
        if (rejectedCount > 0) {
            String aversionConf = rejectedCount >= 5 ? "HIGH" : (rejectedCount >= 2 ? "MEDIUM" : "LOW");
            double aversionConfScore = rejectedCount >= 5 ? 0.85 : (rejectedCount >= 2 ? 0.65 : 0.30);
            String aversionVal = "Brand content has " + rejectedCount + " rejected drafts. Avoid generic motivational phrasing and ensure strong factual grounding.";
            String aversionSummary = "Evidence: " + rejectedCount + " explicitly rejected drafts in feedback history.";
            saveOrUpdateInsight(user, brand, "AVERSION", "REJECTION_PATTERNS", aversionVal, aversionConf, aversionConfScore, aversionSummary, (int) rejectedCount, "BRAND", null);
        }

        return insightRepository.findByBrandIdAndUserIdAndStatus(brand.getId(), user.getId(), "ACTIVE");
    }

    private void saveOrUpdateInsight(
            UserEntity user,
            BrandEntity brand,
            String type,
            String key,
            String value,
            String confidence,
            double confidenceScore,
            String summary,
            int evidenceCount,
            String scope,
            String contentTypeScope
    ) {
        Optional<ContentIntelligenceInsightEntity> existingOpt = insightRepository.findByBrandIdAndUserIdAndInsightKey(brand.getId(), user.getId(), key);
        ContentIntelligenceInsightEntity insight;
        if (existingOpt.isPresent()) {
            insight = existingOpt.get();
            insight.setInsightValue(value);
            insight.setConfidence(confidence);
            insight.setConfidenceScore(confidenceScore);
            insight.setEvidenceSummary(summary);
            insight.setEvidenceCount(evidenceCount);
            insight.setUpdatedAt(OffsetDateTime.now());
        } else {
            insight = ContentIntelligenceInsightEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .brand(brand)
                .type(type)
                .insightKey(key)
                .insightValue(value)
                .confidence(confidence)
                .confidenceScore(confidenceScore)
                .evidenceSummary(summary)
                .evidenceCount(evidenceCount)
                .status("ACTIVE")
                .scope(scope)
                .contentTypeScope(contentTypeScope)
                .createdAt(OffsetDateTime.now())
                .build();
        }
        insightRepository.save(insight);
    }

    @Transactional(readOnly = true)
    public String assembleIntelligencePromptContext(UserEntity user, BrandEntity brand, String contentType) {
        List<ContentIntelligenceInsightEntity> activeInsights = insightRepository.findByBrandIdAndUserIdAndStatus(brand.getId(), user.getId(), "ACTIVE");
        if (activeInsights.isEmpty()) {
            return "No historical intelligence insights recorded yet.";
        }

        // Filter active insights matching brand or content type with MEDIUM or HIGH confidence
        List<ContentIntelligenceInsightEntity> relevant = activeInsights.stream()
            .filter(i -> "HIGH".equalsIgnoreCase(i.getConfidence()) || "MEDIUM".equalsIgnoreCase(i.getConfidence()))
            .filter(i -> i.getContentTypeScope() == null || i.getContentTypeScope().equalsIgnoreCase(contentType))
            .limit(5)
            .collect(Collectors.toList());

        if (relevant.isEmpty()) {
            return "No high-confidence intelligence insights for this content type yet.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Learned Behavioral Intelligence & Insights (Confidence-Rated):\n");
        for (ContentIntelligenceInsightEntity insight : relevant) {
            sb.append("- [").append(insight.getConfidence()).append(" CONFIDENCE - ").append(insight.getType()).append("]: ")
              .append(insight.getInsightValue()).append("\n");
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    public List<ContentIntelligenceInsightEntity> getBrandInsights(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        return insightRepository.findByBrandIdAndUserIdAndStatus(brand.getId(), user.getId(), "ACTIVE");
    }
}
