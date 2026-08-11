package com.app.service;

import com.app.entity.*;
import com.app.repository.BrandMemoryRepository;
import com.app.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandMemoryService {

    private final UserMemoryRepository userMemoryRepository;
    private final BrandMemoryRepository brandMemoryRepository;

    @Transactional(readOnly = true)
    public List<UserMemoryEntity> getUserMemories(UserEntity user) {
        return userMemoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<BrandMemoryEntity> getBrandMemories(UserEntity user, Long brandId) {
        return brandMemoryRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brandId, user.getId());
    }

    @Transactional
    public void recordUserMemoryCandidate(UserEntity user, String memoryType, String value, String source) {
        UserMemoryEntity memory = UserMemoryEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .memoryType(memoryType)
            .value(value)
            .source(source)
            .confidence(1.0)
            .build();
        userMemoryRepository.save(memory);
        log.info("Recorded new User Memory for user [{}]: {}", user.getEmail(), value);
    }

    @Transactional
    public void recordBrandMemoryCandidate(UserEntity user, BrandEntity brand, String memoryType, String value, String source) {
        BrandMemoryEntity memory = BrandMemoryEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(brand)
            .memoryType(memoryType)
            .value(value)
            .source(source)
            .confidence(1.0)
            .build();
        brandMemoryRepository.save(memory);
        log.info("Recorded new Brand Memory for brand [{}] ({}): {}", brand.getName(), brand.getPublicId(), value);
    }

    /**
     * Evaluates explicit feedback to extract permanent memory candidates.
     * Prevents temporary instructions (e.g., "Make this 2 lines shorter") from polluting permanent memory.
     */
    @Transactional
    public void processFeedbackForMemoryExtraction(UserEntity user, BrandEntity brand, String feedbackText) {
        if (feedbackText == null || feedbackText.isBlank()) return;
        String lower = feedbackText.toLowerCase();

        if (lower.contains("avoid") || lower.contains("never use") || lower.contains("dont use") || lower.contains("don't use")) {
            recordBrandMemoryCandidate(user, brand, "AVOID_PHRASE", "Avoid in future content: " + feedbackText, "USER_FEEDBACK");
        } else if (lower.contains("prefer") || lower.contains("always use") || lower.contains("love") || lower.contains("keep")) {
            recordBrandMemoryCandidate(user, brand, "WRITING_STYLE", "Style Preference: " + feedbackText, "USER_FEEDBACK");
        }
    }
}
