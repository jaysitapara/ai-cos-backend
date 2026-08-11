package com.app.service;

import com.app.entity.*;
import com.app.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandContextRetrievalService {

    private final BrandKnowledgeRepository brandKnowledgeRepository;
    private final BrandProductRepository brandProductRepository;
    private final BrandMemoryRepository brandMemoryRepository;
    private final UserMemoryRepository userMemoryRepository;
    private final ApprovedContentRepository approvedContentRepository;
    private final ContentIntelligenceEngine intelligenceEngine;

    @Data
    @Builder
    public static class AssembledBrandContext {
        private String brandIdentityPrompt;
        private String brandGuidelinesPrompt;
        private String factualKnowledgePrompt;
        private String productsServicesPrompt;
        private String learnedMemoriesPrompt;
        private String approvedStyleExamplesPrompt;
        private String contentIntelligencePrompt;
    }

    @Transactional(readOnly = true)
    public AssembledBrandContext retrieveContext(UserEntity user, BrandEntity brand, String contentType) {
        log.info("Retrieving context hierarchy for Brand [{}] ({}), User [{}]", brand.getName(), brand.getPublicId(), user.getEmail());

        // 1. Core Identity & Voice
        StringBuilder identityBuilder = new StringBuilder();
        identityBuilder.append("BRAND NAME: ").append(brand.getName()).append("\n");
        if (brand.getIndustry() != null) identityBuilder.append("INDUSTRY: ").append(brand.getIndustry()).append("\n");
        if (brand.getPositioning() != null) identityBuilder.append("POSITIONING: ").append(brand.getPositioning()).append("\n");
        if (brand.getTargetAudience() != null) identityBuilder.append("TARGET AUDIENCE: ").append(brand.getTargetAudience()).append("\n");
        if (brand.getUsp() != null) identityBuilder.append("USP: ").append(brand.getUsp()).append("\n");

        // 2. Guidelines & Voice
        StringBuilder guidelinesBuilder = new StringBuilder();
        if (brand.getBrandVoice() != null) guidelinesBuilder.append("BRAND VOICE: ").append(brand.getBrandVoice()).append("\n");
        if (brand.getWritingStyle() != null) guidelinesBuilder.append("WRITING STYLE: ").append(brand.getWritingStyle()).append("\n");
        if (brand.getWordsToUse() != null) guidelinesBuilder.append("WORDS TO USE: ").append(brand.getWordsToUse()).append("\n");
        if (brand.getWordsToAvoid() != null) guidelinesBuilder.append("WORDS TO AVOID: ").append(brand.getWordsToAvoid()).append("\n");
        if (brand.getBrandGuidelines() != null) guidelinesBuilder.append("GUIDELINES: ").append(brand.getBrandGuidelines()).append("\n");

        // 3. Factual Knowledge (Capped to top 5 items for token efficiency)
        List<BrandKnowledgeEntity> knowledgeItems = brandKnowledgeRepository.findByBrandIdAndUserIdAndStatusOrderByUpdatedAtDesc(
            brand.getId(), user.getId(), "ACTIVE"
        );
        StringBuilder knowledgeBuilder = new StringBuilder();
        int kCount = 0;
        for (BrandKnowledgeEntity k : knowledgeItems) {
            if (kCount++ >= 5) break;
            knowledgeBuilder.append("- [").append(k.getType()).append("] ").append(k.getTitle()).append(": ").append(k.getContent()).append("\n");
        }

        // 4. Products & Services (Capped to top 5 items)
        List<BrandProductEntity> products = brandProductRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
        StringBuilder productsBuilder = new StringBuilder();
        int pCount = 0;
        for (BrandProductEntity p : products) {
            if (pCount++ >= 5) break;
            productsBuilder.append("- [").append(p.getType()).append("] ").append(p.getName()).append(": ").append(p.getDescription()).append("\n");
        }

        // 5. Learned Memories (User + Brand, capped to top 5 relevant entries each)
        List<BrandMemoryEntity> brandMemories = brandMemoryRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
        List<UserMemoryEntity> userMemories = userMemoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        StringBuilder memoriesBuilder = new StringBuilder();
        int bmCount = 0;
        for (BrandMemoryEntity bm : brandMemories) {
            if (bmCount++ >= 5) break;
            memoriesBuilder.append("- [BRAND MEMORY]: ").append(bm.getValue()).append("\n");
        }
        int umCount = 0;
        for (UserMemoryEntity um : userMemories) {
            if (umCount++ >= 5) break;
            memoriesBuilder.append("- [USER PREFERENCE]: ").append(um.getValue()).append("\n");
        }

        // 6. Approved Content Examples (Style Evidence)
        List<ApprovedContentEntity> approvedExamples = approvedContentRepository.findByBrandIdAndUserIdAndContentTypeOrderByCreatedAtDesc(
            brand.getId(), user.getId(), contentType
        );
        if (approvedExamples.isEmpty()) {
            approvedExamples = approvedContentRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
        }
        StringBuilder styleBuilder = new StringBuilder();
        int count = 0;
        for (ApprovedContentEntity ac : approvedExamples) {
            if (count++ >= 2) break; // Limit to 2 high-value examples to prevent context bloat
            styleBuilder.append("--- APPROVED EXAMPLE: ").append(ac.getTitle()).append(" ---\n");
            styleBuilder.append(ac.getFinalContent()).append("\n\n");
        }

        // 7. Intelligence Insights
        String intelPrompt = intelligenceEngine != null
            ? intelligenceEngine.assembleIntelligencePromptContext(user, brand, contentType)
            : "";

        return AssembledBrandContext.builder()
            .brandIdentityPrompt(identityBuilder.toString())
            .brandGuidelinesPrompt(guidelinesBuilder.toString())
            .factualKnowledgePrompt(knowledgeBuilder.toString())
            .productsServicesPrompt(productsBuilder.toString())
            .learnedMemoriesPrompt(memoriesBuilder.toString())
            .approvedStyleExamplesPrompt(styleBuilder.toString())
            .contentIntelligencePrompt(intelPrompt)
            .build();
    }
}
