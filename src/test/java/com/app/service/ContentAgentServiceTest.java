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

@DisplayName("Content Agent Service Tests")
class ContentAgentServiceTest {

    @Mock private ContentThreadRepository threadRepository;
    @Mock private ContentMessageRepository messageRepository;
    @Mock private ContentBriefRepository briefRepository;
    @Mock private GeneratedContentRepository contentRepository;
    @Mock private ContentVersionRepository versionRepository;
    @Mock private ApprovedContentRepository approvedContentRepository;
    @Mock private ContentFeedbackRepository feedbackRepository;
    @Mock private BrandManagementService brandManagementService;
    @Mock private DynamicContentQuestionEngine questionEngine;
    @Mock private BrandContextRetrievalService contextRetrievalService;
    @Mock private BrandMemoryService memoryService;
    @Mock private AIService aiService;
    @Mock private AiUsageTrackingService aiUsageTrackingService;
    @Mock private AiOutputSanitizerService sanitizerService;

    @InjectMocks
    private ContentAgentService contentAgentService;

    private UserEntity user;
    private BrandEntity brand;
    private ContentThreadEntity thread;
    private UUID threadPublicId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = UserEntity.builder().id(1L).email("user@example.com").build();
        brand = BrandEntity.builder().id(10L).publicId(UUID.randomUUID()).name("Nexus AI").build();
        threadPublicId = UUID.randomUUID();
        thread = ContentThreadEntity.builder()
            .id(100L)
            .publicId(threadPublicId)
            .user(user)
            .brand(brand)
            .title("LinkedIn Post Thread")
            .contentType("LINKEDIN_POST")
            .build();
    }

    @Test
    @DisplayName("Should generate content via AIService and save Version 1")
    void testGenerateContent() {
        when(threadRepository.findByPublicIdAndUserId(threadPublicId, 1L)).thenReturn(Optional.of(thread));

        ContentBriefEntity brief = ContentBriefEntity.builder()
            .id(200L)
            .thread(thread)
            .topic("AI Engineering")
            .structuredBriefJson("{}")
            .build();
        when(briefRepository.findFirstByThreadIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(brief));

        BrandContextRetrievalService.AssembledBrandContext context = BrandContextRetrievalService.AssembledBrandContext.builder()
            .brandIdentityPrompt("Nexus AI")
            .brandGuidelinesPrompt("Authoritative")
            .factualKnowledgePrompt("")
            .productsServicesPrompt("")
            .learnedMemoriesPrompt("")
            .approvedStyleExamplesPrompt("")
            .build();
        when(contextRetrievalService.retrieveContext(user, brand, "LINKEDIN_POST")).thenReturn(context);

        when(sanitizerService.sanitizeOutput(anyString())).thenAnswer(i -> i.getArgument(0));
        AiCompletionResponse aiResp = new AiCompletionResponse(
            "Here is the generated LinkedIn post body...", "gemini-1.5-flash", "stop", 150, 200
        );
        when(aiService.generateCompletion(any())).thenReturn(aiResp);

        GeneratedContentEntity content = GeneratedContentEntity.builder().id(300L).title("LinkedIn Post Thread").contentType("LINKEDIN_POST").build();
        when(contentRepository.save(any())).thenReturn(content);

        ContentVersionEntity v1 = ContentVersionEntity.builder().id(400L).versionNumber(1).body("Here is the generated LinkedIn post body...").build();
        when(versionRepository.save(any())).thenReturn(v1);

        GeneratedContentEntity result = contentAgentService.generateContent(user, threadPublicId);

        assertNotNull(result);
        verify(aiService, times(1)).generateCompletion(any());
        verify(aiUsageTrackingService, times(1)).logAiUsage(eq(user), any(), any(), any(), any(), eq("gemini-1.5-flash"), eq("CONTENT_AGENT_GENERATION"), eq("CONTENT_AGENT"), eq(150), eq(200), anyLong(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Should submit approval feedback and save approved content snapshot")
    void testApproveContent() {
        GeneratedContentEntity content = GeneratedContentEntity.builder()
            .id(300L)
            .publicId(UUID.randomUUID())
            .user(user)
            .brand(brand)
            .contentType("LINKEDIN_POST")
            .title("Launch Post")
            .build();
        when(contentRepository.findByPublicIdAndUserId(content.getPublicId(), 1L)).thenReturn(Optional.of(content));

        ContentVersionEntity v1 = ContentVersionEntity.builder().id(400L).body("Approved post body").build();
        when(versionRepository.findFirstByContentIdOrderByVersionNumberDesc(300L)).thenReturn(Optional.of(v1));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContentFeedbackEntity fb = contentAgentService.submitFeedback(user, content.getPublicId(), "APPROVE", 5, "Great post!");

        assertNotNull(fb);
        assertEquals("APPROVE", fb.getFeedbackType());
        verify(approvedContentRepository, times(1)).save(any(ApprovedContentEntity.class));
        verify(memoryService, times(1)).processFeedbackForMemoryExtraction(user, brand, "Great post!");
    }
}
