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
public class ContentAnalyticsService {

    private final ContentAnalyticsRepository analyticsRepository;
    private final ContentBehaviorEventRepository eventRepository;
    private final ContentPerformanceSnapshotRepository snapshotRepository;
    private final BrandManagementService brandManagementService;

    @Transactional
    public void recordBehaviorEvent(UserEntity user, BrandEntity brand, GeneratedContentEntity content, String eventType, String details) {
        ContentBehaviorEventEntity event = ContentBehaviorEventEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(brand)
            .content(content)
            .eventType(eventType)
            .details(details)
            .createdAt(OffsetDateTime.now())
            .build();
        eventRepository.save(event);
        log.info("Recorded behavioral event [{}] for brand [{}]", eventType, brand.getName());
    }

    @Transactional
    public ContentAnalyticsEntity createOrUpdateAnalyticsRecord(UserEntity user, BrandEntity brand, GeneratedContentEntity content, String contentType, String topic, String platform, String status) {
        Optional<ContentAnalyticsEntity> existingOpt = analyticsRepository.findByContentIdAndUserId(content.getId(), user.getId());
        ContentAnalyticsEntity record;
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setStatus(status);
            if (topic != null && !topic.isBlank()) record.setTopic(topic);
            if (platform != null && !platform.isBlank()) record.setPlatform(platform);
        } else {
            record = ContentAnalyticsEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .brand(brand)
                .content(content)
                .contentType(contentType)
                .topic(topic)
                .platform(platform != null && !platform.isBlank() ? platform : "OTHER")
                .status(status != null ? status : "DRAFT")
                .editCount(0)
                .versionCount(1)
                .performanceScore(0.0)
                .generatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        }
        ContentAnalyticsEntity saved = analyticsRepository.save(record);
        recordBehaviorEvent(user, brand, content, "CONTENT_GENERATED", "Content artifact generated: " + contentType);
        return saved;
    }

    @Transactional
    public void updateContentStatusAndVersion(UserEntity user, GeneratedContentEntity content, String status, ContentVersionEntity version) {
        Optional<ContentAnalyticsEntity> analyticsOpt = analyticsRepository.findByContentIdAndUserId(content.getId(), user.getId());
        if (analyticsOpt.isPresent()) {
            ContentAnalyticsEntity analytics = analyticsOpt.get();
            analytics.setStatus(status);
            if (version != null) {
                analytics.setContentVersion(version);
                analytics.setVersionCount(version.getVersionNumber());
            }
            if ("APPROVED".equalsIgnoreCase(status)) {
                analytics.setApprovedAt(OffsetDateTime.now());
                recordBehaviorEvent(user, analytics.getBrand(), content, "CONTENT_APPROVED", "Content approved by user");
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                recordBehaviorEvent(user, analytics.getBrand(), content, "CONTENT_REJECTED", "Content rejected by user");
            } else if ("PUBLISHED".equalsIgnoreCase(status)) {
                analytics.setPublishedAt(OffsetDateTime.now());
                recordBehaviorEvent(user, analytics.getBrand(), content, "CONTENT_PUBLISHED", "Content published to platform: " + analytics.getPlatform());
            }
            analyticsRepository.save(analytics);
        }
    }

    @Transactional
    public void incrementEditCount(UserEntity user, GeneratedContentEntity content) {
        Optional<ContentAnalyticsEntity> analyticsOpt = analyticsRepository.findByContentIdAndUserId(content.getId(), user.getId());
        if (analyticsOpt.isPresent()) {
            ContentAnalyticsEntity analytics = analyticsOpt.get();
            analytics.setEditCount(analytics.getEditCount() + 1);
            analytics.setVersionCount(analytics.getVersionCount() + 1);
            analyticsRepository.save(analytics);
            recordBehaviorEvent(user, analytics.getBrand(), content, "CONTENT_EDITED", "User saved edit snapshot version " + analytics.getVersionCount());
        }
    }

    @Transactional
    public ContentPerformanceSnapshotEntity recordPerformanceSnapshot(
            UserEntity user,
            UUID analyticsPublicId,
            String platform,
            Long impressions,
            Long reach,
            Long likes,
            Long comments,
            Long shares,
            Long saves,
            Long clicks,
            Long conversions
    ) {
        ContentAnalyticsEntity analytics = analyticsRepository.findByPublicIdAndUserId(analyticsPublicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Content analytics record not found for public ID: " + analyticsPublicId));

        long totalEngagements = (likes != null ? likes : 0)
            + (comments != null ? comments : 0)
            + (shares != null ? shares : 0)
            + (saves != null ? saves : 0)
            + (clicks != null ? clicks : 0);

        long baseImp = (impressions != null && impressions > 0) ? impressions : 1;
        double engagementRate = ((double) totalEngagements / baseImp) * 100.0;

        ContentPerformanceSnapshotEntity snapshot = ContentPerformanceSnapshotEntity.builder()
            .publicId(UUID.randomUUID())
            .contentAnalytics(analytics)
            .platform(platform != null ? platform : analytics.getPlatform())
            .impressions(impressions != null ? impressions : 0L)
            .reach(reach != null ? reach : 0L)
            .likes(likes != null ? likes : 0L)
            .comments(comments != null ? comments : 0L)
            .shares(shares != null ? shares : 0L)
            .saves(saves != null ? saves : 0L)
            .clicks(clicks != null ? clicks : 0L)
            .conversions(conversions != null ? conversions : 0L)
            .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
            .snapshotAt(OffsetDateTime.now())
            .build();

        ContentPerformanceSnapshotEntity saved = snapshotRepository.save(snapshot);

        // Update overall normalized performance score
        double normScore = Math.min(100.0, (engagementRate * 10.0) + ((conversions != null ? conversions : 0) * 5.0));
        analytics.setPerformanceScore(Math.round(normScore * 10.0) / 10.0);
        analytics.setStatus("PUBLISHED");
        if (analytics.getPublishedAt() == null) analytics.setPublishedAt(OffsetDateTime.now());
        analyticsRepository.save(analytics);

        recordBehaviorEvent(user, analytics.getBrand(), analytics.getContent(), "PERFORMANCE_SNAPSHOT_ADDED", "Manual performance metrics added: " + totalEngagements + " engagements");
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBrandAnalyticsOverview(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        List<ContentAnalyticsEntity> records = analyticsRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());

        int totalContent = records.size();
        long approvedCount = records.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()) || "PUBLISHED".equalsIgnoreCase(r.getStatus())).count();
        long rejectedCount = records.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).count();
        double approvalRate = totalContent > 0 ? (double) approvedCount / totalContent * 100.0 : 0.0;

        double avgEdits = records.stream().mapToInt(ContentAnalyticsEntity::getEditCount).average().orElse(0.0);
        double avgVersions = records.stream().mapToInt(ContentAnalyticsEntity::getVersionCount).average().orElse(1.0);
        double avgPerformanceScore = records.stream().mapToDouble(ContentAnalyticsEntity::getPerformanceScore).filter(s -> s > 0).average().orElse(0.0);

        Map<String, Long> contentTypeCounts = records.stream()
            .collect(Collectors.groupingBy(ContentAnalyticsEntity::getContentType, Collectors.counting()));

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("brandName", brand.getName());
        overview.put("brandPublicId", brand.getPublicId().toString());
        overview.put("totalContent", totalContent);
        overview.put("approvedCount", approvedCount);
        overview.put("rejectedCount", rejectedCount);
        overview.put("approvalRate", Math.round(approvalRate * 10.0) / 10.0);
        overview.put("avgEditsPerPost", Math.round(avgEdits * 10.0) / 10.0);
        overview.put("avgVersionsPerPost", Math.round(avgVersions * 10.0) / 10.0);
        overview.put("avgPerformanceScore", Math.round(avgPerformanceScore * 10.0) / 10.0);
        overview.put("contentTypeBreakdown", contentTypeCounts);
        return overview;
    }

    @Transactional(readOnly = true)
    public List<ContentAnalyticsEntity> getBrandContentAnalyticsList(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        return analyticsRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
    }
}
