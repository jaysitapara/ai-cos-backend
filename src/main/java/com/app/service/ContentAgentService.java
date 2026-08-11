package com.app.service;

import com.app.entity.*;
import com.app.provider.ai.AiCompletionRequest;
import com.app.provider.ai.AiCompletionResponse;
import com.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAgentService {

    private final ContentThreadRepository threadRepository;
    private final ContentMessageRepository messageRepository;
    private final ContentBriefRepository briefRepository;
    private final GeneratedContentRepository contentRepository;
    private final ContentVersionRepository versionRepository;
    private final ApprovedContentRepository approvedContentRepository;
    private final ContentFeedbackRepository feedbackRepository;
    private final BrandManagementService brandManagementService;
    private final DynamicContentQuestionEngine questionEngine;
    private final BrandContextRetrievalService contextRetrievalService;
    private final BrandMemoryService memoryService;
    private final AIService aiService;
    private final AiUsageTrackingService aiUsageTrackingService;
    private final AiOutputSanitizerService sanitizerService;
    private final ContentAnalyticsService analyticsService;

    // ─── Thread Management ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ContentThreadEntity> getThreadsForBrand(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        return threadRepository.findByUserIdAndBrandIdOrderByUpdatedAtDesc(user.getId(), brand.getId());
    }

    @Transactional
    public ContentThreadEntity createThread(UserEntity user, UUID brandPublicId, String title, String contentType) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        ContentThreadEntity thread = ContentThreadEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(brand)
            .title(title != null && !title.isBlank() ? title : "New " + contentType + " Thread")
            .contentType(contentType != null ? contentType : "LINKEDIN_POST")
            .status("ACTIVE")
            .currentStep(1)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
        ContentThreadEntity saved = threadRepository.save(thread);

        // System message welcoming user
        ContentMessageEntity welcomeMsg = ContentMessageEntity.builder()
            .publicId(UUID.randomUUID())
            .thread(saved)
            .role("ASSISTANT")
            .messageType("QUESTION")
            .content("Welcome! Let's build a high-impact " + saved.getContentType() + " for " + brand.getName() + ".")
            .createdAt(OffsetDateTime.now())
            .build();
        messageRepository.save(welcomeMsg);

        return saved;
    }

    @Transactional(readOnly = true)
    public ContentThreadEntity getThreadByPublicId(UserEntity user, UUID publicId) {
        return threadRepository.findByPublicIdAndUserId(publicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Thread not found or access denied for ID: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<ContentMessageEntity> getThreadMessages(UserEntity user, UUID threadPublicId) {
        ContentThreadEntity thread = getThreadByPublicId(user, threadPublicId);
        return messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
    }

    // ─── Content Brief Building ───────────────────────────────────────────────
    @Transactional
    public ContentBriefEntity createContentBrief(UserEntity user, UUID threadPublicId) {
        ContentThreadEntity thread = getThreadByPublicId(user, threadPublicId);
        List<ContentQuestionStateEntity> answers = questionEngine.getSavedAnswers(thread.getId());

        Map<String, String> briefMap = new HashMap<>();
        String topic = null, goal = null, audience = null, tone = null, cta = null, additional = null;

        for (ContentQuestionStateEntity ans : answers) {
            String val = ans.getCustomValue() != null && !ans.getCustomValue().isBlank()
                ? ans.getCustomValue() : ans.getSelectedOption();
            briefMap.put(ans.getQuestionKey(), val);

            switch (ans.getQuestionKey()) {
                case "topic" -> topic = val;
                case "goal" -> goal = val;
                case "targetAudience" -> audience = val;
                case "tone" -> tone = val;
                case "cta" -> cta = val;
                default -> {
                    if (additional == null) additional = ans.getQuestionTitle() + ": " + val;
                    else additional += "\n" + ans.getQuestionTitle() + ": " + val;
                }
            }
        }

        ContentBriefEntity brief = ContentBriefEntity.builder()
            .publicId(UUID.randomUUID())
            .thread(thread)
            .contentType(thread.getContentType())
            .topic(topic)
            .goal(goal)
            .targetAudience(audience)
            .keyMessage(topic)
            .tone(tone)
            .cta(cta)
            .additionalInstructions(additional)
            .structuredBriefJson(briefMap.toString())
            .createdAt(OffsetDateTime.now())
            .build();

        return briefRepository.save(brief);
    }

    // ─── Content Generation ───────────────────────────────────────────────────
    @Transactional
    public GeneratedContentEntity generateContent(UserEntity user, UUID threadPublicId) {
        ContentThreadEntity thread = getThreadByPublicId(user, threadPublicId);
        BrandEntity brand = thread.getBrand();

        ContentBriefEntity brief = briefRepository.findFirstByThreadIdOrderByCreatedAtDesc(thread.getId())
            .orElseGet(() -> createContentBrief(user, threadPublicId));

        // Context Assembly
        BrandContextRetrievalService.AssembledBrandContext context = contextRetrievalService.retrieveContext(
            user, brand, thread.getContentType()
        );

        String systemPrompt = """
            You are an elite Personal Brand Content Strategist and Copywriter for '%s'.
            Your mission is to generate high-performing, authentic %s content aligned with the brand voice.

            === BRAND CONTEXT ===
            %s

            === BRAND GUIDELINES ===
            %s

            === FACTUAL KNOWLEDGE BASE ===
            %s

            === PRODUCTS & SERVICES ===
            %s

            === LEARNED MEMORIES & PREFERENCES ===
            %s

            === APPROVED HIGH-PERFORMING STYLE EXAMPLES ===
            %s
            """.formatted(
                brand.getName(),
                thread.getContentType(),
                context.getBrandIdentityPrompt(),
                context.getBrandGuidelinesPrompt(),
                context.getFactualKnowledgePrompt(),
                context.getProductsServicesPrompt(),
                context.getLearnedMemoriesPrompt(),
                context.getApprovedStyleExamplesPrompt()
            );

        String userPrompt = """
            Generate a complete, high-converting %s based strictly on this content brief:

            - Topic / Core Takeaway: %s
            - Objective / Goal: %s
            - Target Audience: %s
            - Desired Tone: %s
            - Call to Action: %s
            - Additional Notes: %s

            IMPORTANT: Provide ONLY the final post body. Do not include meta comments, markdown tags, or commentary.
            """.formatted(
                thread.getContentType(),
                brief.getTopic(),
                brief.getGoal(),
                brief.getTargetAudience(),
                brief.getTone(),
                brief.getCta(),
                brief.getAdditionalInstructions()
            );

        AiCompletionRequest aiReq = new AiCompletionRequest(
            userPrompt,
            systemPrompt,
            null,
            0.4,
            1200,
            null
        );

        log.info("Executing Content Agent generation via AIService for user [{}]...", user.getEmail());
        long start = System.currentTimeMillis();
        AiCompletionResponse response = aiService.generateCompletion(aiReq);
        long latency = System.currentTimeMillis() - start;

        aiUsageTrackingService.logAiUsage(
            user,
            null,
            null,
            UUID.randomUUID().toString(),
            "AI_COS",
            response.getModelName(),
            "CONTENT_AGENT_GENERATION",
            "CONTENT_AGENT",
            (int) response.getPromptTokens(),
            (int) response.getCompletionTokens(),
            latency,
            "SUCCESS",
            null
        );

        String generatedText = sanitizerService.sanitizeOutput(response.getContent());

        // Create Content Entity
        GeneratedContentEntity content = GeneratedContentEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(brand)
            .thread(thread)
            .brief(brief)
            .contentType(thread.getContentType())
            .title(thread.getTitle())
            .status("DRAFT")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
        GeneratedContentEntity savedContent = contentRepository.save(content);

        // Save Version 1 snapshot
        ContentVersionEntity v1 = ContentVersionEntity.builder()
            .publicId(UUID.randomUUID())
            .content(savedContent)
            .versionNumber(1)
            .body(generatedText)
            .changeSource("AI_INITIAL")
            .changeSummary("Initial AI Generation via " + response.getModelName())
            .createdAt(OffsetDateTime.now())
            .build();
        ContentVersionEntity savedVersion = versionRepository.save(v1);

        // Record Analytics
        if (analyticsService != null) {
            analyticsService.createOrUpdateAnalyticsRecord(user, brand, savedContent, thread.getContentType(), brief.getTopic(), "OTHER", "DRAFT");
        }

        savedContent.setCurrentVersionId(savedVersion.getId());
        contentRepository.save(savedContent);

        // Add message to thread
        ContentMessageEntity assistantMsg = ContentMessageEntity.builder()
            .publicId(UUID.randomUUID())
            .thread(thread)
            .role("ASSISTANT")
            .messageType("GENERATED_OUTPUT")
            .content(generatedText)
            .createdAt(OffsetDateTime.now())
            .build();
        messageRepository.save(assistantMsg);

        return savedContent;
    }

    // ─── Manual Save / Edit ─────────────────────────────────────────────────
    @Transactional
    public ContentVersionEntity saveUserEdit(UserEntity user, UUID contentPublicId, String updatedBody) {
        GeneratedContentEntity content = contentRepository.findByPublicIdAndUserId(contentPublicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Content not found or access denied for ID: " + contentPublicId));

        ContentVersionEntity current = versionRepository.findFirstByContentIdOrderByVersionNumberDesc(content.getId())
            .orElseThrow(() -> new IllegalStateException("No current version found"));

        int nextVerNum = current.getVersionNumber() + 1;

        ContentVersionEntity newVer = ContentVersionEntity.builder()
            .publicId(UUID.randomUUID())
            .content(content)
            .versionNumber(nextVerNum)
            .parentVersionId(current.getId())
            .body(updatedBody)
            .changeSource("USER_EDIT")
            .changeSummary("Manual user edit")
            .createdAt(OffsetDateTime.now())
            .build();

        ContentVersionEntity savedVer = versionRepository.save(newVer);
        content.setCurrentVersionId(savedVer.getId());
        content.setStatus("EDITED");
        content.setUpdatedAt(OffsetDateTime.now());
        contentRepository.save(content);

        if (analyticsService != null) {
            analyticsService.incrementEditCount(user, content);
        }

        return savedVer;
    }

    // ─── Targeted AI Rewrites ────────────────────────────────────────────────
    @Transactional
    public ContentVersionEntity executeTargetedRewrite(UserEntity user, UUID contentPublicId, String action, String customInstruction) {
        GeneratedContentEntity content = contentRepository.findByPublicIdAndUserId(contentPublicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Content not found or access denied for ID: " + contentPublicId));

        ContentVersionEntity current = versionRepository.findFirstByContentIdOrderByVersionNumberDesc(content.getId())
            .orElseThrow(() -> new IllegalStateException("No current version found"));

        String actionUpper = action != null ? action.toUpperCase() : "REWRITE";
        String actionInstruction = switch (actionUpper) {
            case "SHORTEN" -> "Make the post more concise while preserving core hook and value proposition.";
            case "EXPAND" -> "Expand on key points with deeper insights, actionable examples, and engaging storytelling.";
            case "IMPROVE_HOOK" -> "Rewrite the opening 1-2 lines to create an irresistible, curiosity-inducing hook.";
            case "VARIATION" -> "Create an alternative version with a completely different angle and style.";
            default -> customInstruction != null ? customInstruction : "Rewrite to improve flow and engagement.";
        };

        int maxTokens = switch (actionUpper) {
            case "SHORTEN" -> 500;
            case "IMPROVE_HOOK" -> 400;
            case "EXPAND" -> 1500;
            default -> 1000;
        };

        String systemPrompt = "You are an expert Content Strategist for brand '%s'. Refine the content.".formatted(content.getBrand().getName());
        String userPrompt = """
            Here is the current content draft:

            %s

            === ACTION INSTRUCTION ===
            %s

            Return ONLY the refined content body without commentary.
            """.formatted(current.getBody(), actionInstruction);

        AiCompletionRequest aiReq = new AiCompletionRequest(
            userPrompt, systemPrompt, null, 0.4, maxTokens, null
        );

        long start = System.currentTimeMillis();
        AiCompletionResponse response = aiService.generateCompletion(aiReq);
        long latency = System.currentTimeMillis() - start;

        aiUsageTrackingService.logAiUsage(
            user, null, null, UUID.randomUUID().toString(), "AI_COS",
            response.getModelName(), "CONTENT_AGENT_REWRITE", "CONTENT_AGENT",
            (int) response.getPromptTokens(), (int) response.getCompletionTokens(),
            latency, "SUCCESS", null
        );

        String rewrittenText = sanitizerService.sanitizeOutput(response.getContent());

        int nextVerNum = current.getVersionNumber() + 1;
        ContentVersionEntity newVer = ContentVersionEntity.builder()
            .publicId(UUID.randomUUID())
            .content(content)
            .versionNumber(nextVerNum)
            .parentVersionId(current.getId())
            .body(rewrittenText)
            .changeSource("AI_" + (action != null ? action.toUpperCase() : "REWRITE"))
            .changeSummary("AI action: " + actionInstruction)
            .createdAt(OffsetDateTime.now())
            .build();

        ContentVersionEntity savedVer = versionRepository.save(newVer);
        content.setCurrentVersionId(savedVer.getId());
        content.setUpdatedAt(OffsetDateTime.now());
        contentRepository.save(content);

        if (analyticsService != null) {
            analyticsService.incrementEditCount(user, content);
        }

        return savedVer;
    }

    // ─── Feedback & Approval ──────────────────────────────────────────────────
    @Transactional
    public ContentFeedbackEntity submitFeedback(UserEntity user, UUID contentPublicId, String feedbackType, Integer rating, String feedbackText) {
        GeneratedContentEntity content = contentRepository.findByPublicIdAndUserId(contentPublicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Content not found or access denied for ID: " + contentPublicId));

        ContentVersionEntity current = versionRepository.findFirstByContentIdOrderByVersionNumberDesc(content.getId()).orElse(null);

        ContentFeedbackEntity fb = ContentFeedbackEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(content.getBrand())
            .content(content)
            .versionId(current != null ? current.getId() : null)
            .feedbackType(feedbackType != null ? feedbackType : "COMMENT")
            .rating(rating)
            .feedbackText(feedbackText != null ? feedbackText : "")
            .createdAt(OffsetDateTime.now())
            .build();

        ContentFeedbackEntity savedFb = feedbackRepository.save(fb);

        if ("APPROVE".equalsIgnoreCase(feedbackType)) {
            content.setStatus("APPROVED");
            contentRepository.save(content);

            // Copy to Approved Content for style evidence
            if (current != null) {
                ApprovedContentEntity ac = ApprovedContentEntity.builder()
                    .publicId(UUID.randomUUID())
                    .user(user)
                    .brand(content.getBrand())
                    .contentType(content.getContentType())
                    .title(content.getTitle())
                    .finalContent(current.getBody())
                    .performanceRating(rating != null ? rating : 5)
                    .createdAt(OffsetDateTime.now())
                    .build();
                approvedContentRepository.save(ac);
                log.info("Saved Approved Content snapshot for brand [{}]", content.getBrand().getName());
            }

            if (analyticsService != null) {
                analyticsService.updateContentStatusAndVersion(user, content, "APPROVED", current);
            }
        } else if ("REJECT".equalsIgnoreCase(feedbackType)) {
            content.setStatus("REJECTED");
            contentRepository.save(content);

            if (analyticsService != null) {
                analyticsService.updateContentStatusAndVersion(user, content, "REJECTED", current);
            }
        }

        // Process for permanent memory candidate
        memoryService.processFeedbackForMemoryExtraction(user, content.getBrand(), feedbackText);

        return savedFb;
    }

    // ─── Version & Library Queries ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ContentVersionEntity> getContentVersions(UserEntity user, UUID contentPublicId) {
        GeneratedContentEntity content = contentRepository.findByPublicIdAndUserId(contentPublicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Content not found or access denied for ID: " + contentPublicId));
        return versionRepository.findByContentIdOrderByVersionNumberDesc(content.getId());
    }

    @Transactional(readOnly = true)
    public List<GeneratedContentEntity> getContentLibrary(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = brandManagementService.getBrandByPublicId(user, brandPublicId);
        return contentRepository.findByUserIdAndBrandIdOrderByUpdatedAtDesc(user.getId(), brand.getId());
    }
}
