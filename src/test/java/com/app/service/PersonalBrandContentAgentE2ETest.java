package com.app.service;

import com.app.entity.*;
import com.app.provider.ai.AiCompletionResponse;
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

@DisplayName("Personal Brand AI Content Agent — Full E2E Journey Validation")
class PersonalBrandContentAgentE2ETest {

    @Mock private BrandRepository brandRepository;
    @Mock private BrandProductRepository brandProductRepository;
    @Mock private BrandKnowledgeRepository brandKnowledgeRepository;
    @Mock private ContentThreadRepository threadRepository;
    @Mock private ContentMessageRepository messageRepository;
    @Mock private ContentQuestionStateRepository questionStateRepository;
    @Mock private ContentBriefRepository briefRepository;
    @Mock private GeneratedContentRepository contentRepository;
    @Mock private ContentVersionRepository versionRepository;
    @Mock private UserMemoryRepository userMemoryRepository;
    @Mock private BrandMemoryRepository brandMemoryRepository;
    @Mock private ApprovedContentRepository approvedContentRepository;
    @Mock private ContentFeedbackRepository feedbackRepository;
    @Mock private AIService aiService;
    @Mock private AiUsageTrackingService aiUsageTrackingService;

    @Mock private ContentAnalyticsService analyticsService;

    @InjectMocks private BrandManagementService brandManagementService;
    @InjectMocks private DynamicContentQuestionEngine questionEngine;
    @InjectMocks private BrandContextRetrievalService contextRetrievalService;
    @InjectMocks private BrandMemoryService memoryService;
    @InjectMocks private ContentAgentService contentAgentService;

    private UserEntity userA;
    private UserEntity userB;
    private BrandEntity brandA;
    private UUID brandPublicId;
    private ContentThreadEntity thread1;
    private UUID threadPublicId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userA = UserEntity.builder().id(1L).email("userA@company.com").build();
        userB = UserEntity.builder().id(2L).email("userB@company.com").build();

        brandPublicId = UUID.randomUUID();
        brandA = BrandEntity.builder()
            .id(10L)
            .publicId(brandPublicId)
            .user(userA)
            .name("Apex Cyber")
            .industry("Cybersecurity")
            .positioning("Zero-Trust Security Leader")
            .targetAudience("CISOs & Tech Executives")
            .brandVoice("Authoritative + Direct")
            .wordsToUse("zero-trust, resiliency, defense")
            .wordsToAvoid("synergy, silver bullet")
            .build();

        threadPublicId = UUID.randomUUID();
        thread1 = ContentThreadEntity.builder()
            .id(100L)
            .publicId(threadPublicId)
            .user(userA)
            .brand(brandA)
            .title("Zero Trust Launch Post")
            .contentType("LINKEDIN_POST")
            .build();

        // Inject dependencies into ContentAgentService
        contentAgentService = new ContentAgentService(
            threadRepository,
            messageRepository,
            briefRepository,
            contentRepository,
            versionRepository,
            approvedContentRepository,
            feedbackRepository,
            brandManagementService,
            questionEngine,
            contextRetrievalService,
            memoryService,
            aiService,
            aiUsageTrackingService,
            new AiOutputSanitizerService(),
            analyticsService
        );
    }

    @Test
    @DisplayName("E2E Step 1-32: Complete Brand Creation, Questions, Brief, Generation, Editing, Action, Approval, Memory, Context Retrieval & User Isolation")
    void testFullContentAgentUserJourney() {
        // Step 1: Create Brand
        when(brandRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        BrandEntity createdBrand = brandManagementService.createBrand(userA, brandA);
        assertEquals("Apex Cyber", createdBrand.getName());

        // Step 2: Dynamic Questions & Derived Options
        var questions = questionEngine.getQuestionsForContentType("LINKEDIN_POST", createdBrand);
        assertFalse(questions.isEmpty());
        assertTrue(questions.get(0).getKey().equals("topic"));

        // Step 3: Stateful Answer Persistence & Custom Write-in
        when(threadRepository.findByPublicIdAndUserId(threadPublicId, 1L)).thenReturn(Optional.of(thread1));
        when(questionStateRepository.findByThreadIdAndQuestionKey(100L, "topic")).thenReturn(Optional.empty());
        when(questionStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Map<String, String>> answers = Map.of(
            "topic", Map.of("title", "Topic", "selectedOption", "Other", "customValue", "Zero-Trust Architecture Trends")
        );
        var savedAnswers = questionEngine.saveAnswers(thread1, answers);
        assertEquals("Zero-Trust Architecture Trends", savedAnswers.get(0).getCustomValue());

        // Step 4: Content Brief Creation
        when(questionStateRepository.findByThreadIdOrderByOrderIndexAsc(100L)).thenReturn(savedAnswers);
        when(briefRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContentBriefEntity brief = contentAgentService.createContentBrief(userA, threadPublicId);
        assertNotNull(brief);
        assertEquals("Zero-Trust Architecture Trends", brief.getTopic());

        // Step 5: Content Generation via AIService
        when(briefRepository.findFirstByThreadIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(brief));
        when(brandKnowledgeRepository.findByBrandIdAndUserIdAndStatusOrderByUpdatedAtDesc(10L, 1L, "ACTIVE")).thenReturn(Collections.emptyList());
        when(brandProductRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(Collections.emptyList());
        when(brandMemoryRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(Collections.emptyList());
        when(userMemoryRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(approvedContentRepository.findByBrandIdAndUserIdAndContentTypeOrderByCreatedAtDesc(10L, 1L, "LINKEDIN_POST")).thenReturn(Collections.emptyList());

        AiCompletionResponse aiResp1 = new AiCompletionResponse(
            "Zero Trust is no longer optional. Here are 3 architecture principles for CISOs...",
            "gemini-1.5-flash", "stop", 120, 180
        );
        when(aiService.generateCompletion(any())).thenReturn(aiResp1);

        GeneratedContentEntity content = GeneratedContentEntity.builder()
            .id(300L)
            .publicId(UUID.randomUUID())
            .user(userA)
            .brand(brandA)
            .thread(thread1)
            .brief(brief)
            .contentType("LINKEDIN_POST")
            .title("Zero Trust Launch Post")
            .status("DRAFT")
            .build();
        when(contentRepository.save(any())).thenReturn(content);

        ContentVersionEntity v1 = ContentVersionEntity.builder()
            .id(400L)
            .publicId(UUID.randomUUID())
            .content(content)
            .versionNumber(1)
            .body("Zero Trust is no longer optional. Here are 3 architecture principles for CISOs...")
            .changeSource("AI_INITIAL")
            .build();
        when(versionRepository.save(any())).thenReturn(v1);

        GeneratedContentEntity genResult = contentAgentService.generateContent(userA, threadPublicId);
        assertNotNull(genResult);
        verify(aiService, times(1)).generateCompletion(any());

        // Step 6: Manual Edit & Version 2
        when(contentRepository.findByPublicIdAndUserId(content.getPublicId(), 1L)).thenReturn(Optional.of(content));
        when(versionRepository.findFirstByContentIdOrderByVersionNumberDesc(300L)).thenReturn(Optional.of(v1));

        ContentVersionEntity v2 = ContentVersionEntity.builder()
            .id(401L)
            .content(content)
            .versionNumber(2)
            .body("Zero Trust is essential. Here are 3 principles every CISO must adopt...")
            .changeSource("USER_EDIT")
            .build();
        when(versionRepository.save(any())).thenReturn(v2);

        ContentVersionEntity savedUserEdit = contentAgentService.saveUserEdit(userA, content.getPublicId(), "Zero Trust is essential...");
        assertEquals(2, savedUserEdit.getVersionNumber());

        // Step 7: Targeted AI Rewrite Action (Improve Hook) & Version 3
        when(versionRepository.findFirstByContentIdOrderByVersionNumberDesc(300L)).thenReturn(Optional.of(v2));
        AiCompletionResponse aiResp2 = new AiCompletionResponse(
            "Are you relying on perimeter security in 2026? Big mistake.",
            "gpt-4o", "stop", 80, 50
        );
        when(aiService.generateCompletion(any())).thenReturn(aiResp2);

        ContentVersionEntity v3 = ContentVersionEntity.builder()
            .id(402L)
            .content(content)
            .versionNumber(3)
            .body("Are you relying on perimeter security in 2026? Big mistake.")
            .changeSource("AI_IMPROVE_HOOK")
            .build();
        when(versionRepository.save(any())).thenReturn(v3);

        ContentVersionEntity targetedRewrite = contentAgentService.executeTargetedRewrite(userA, content.getPublicId(), "IMPROVE_HOOK", null);
        assertEquals(3, targetedRewrite.getVersionNumber());

        // Step 8: Approval, Feedback & Memory Extraction Candidate
        when(versionRepository.findFirstByContentIdOrderByVersionNumberDesc(300L)).thenReturn(Optional.of(v3));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContentFeedbackEntity feedback = contentAgentService.submitFeedback(
            userA, content.getPublicId(), "APPROVE", 5, "Avoid generic motivational hooks. Keep future posts conversational and use stronger hooks."
        );
        assertNotNull(feedback);

        // Verify approved content saved as style evidence
        verify(approvedContentRepository, times(1)).save(any(ApprovedContentEntity.class));

        // Verify memory candidate extracted
        verify(brandMemoryRepository, times(1)).save(any(BrandMemoryEntity.class));

        // Step 9: Verify Context Retrieval for Second Generation Includes Learned Memory & Style Evidence
        ApprovedContentEntity approvedExample = ApprovedContentEntity.builder()
            .title("Zero Trust Launch Post")
            .finalContent("Are you relying on perimeter security in 2026? Big mistake.")
            .build();
        BrandMemoryEntity brandMemory = BrandMemoryEntity.builder()
            .value("Avoid in future content: Avoid generic motivational hooks.")
            .build();

        when(brandMemoryRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(10L, 1L)).thenReturn(List.of(brandMemory));
        when(approvedContentRepository.findByBrandIdAndUserIdAndContentTypeOrderByCreatedAtDesc(10L, 1L, "LINKEDIN_POST")).thenReturn(List.of(approvedExample));

        var assembledContext = contextRetrievalService.retrieveContext(userA, brandA, "LINKEDIN_POST");
        assertTrue(assembledContext.getLearnedMemoriesPrompt().contains("Avoid in future content"));
        assertTrue(assembledContext.getApprovedStyleExamplesPrompt().contains("perimeter security in 2026"));

        // Step 10: Server-side User Isolation
        when(brandRepository.findByPublicIdAndUserId(brandPublicId, 2L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> brandManagementService.getBrandByPublicId(userB, brandPublicId));

        when(threadRepository.findByPublicIdAndUserId(threadPublicId, 2L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> contentAgentService.getThreadByPublicId(userB, threadPublicId));
    }
}
