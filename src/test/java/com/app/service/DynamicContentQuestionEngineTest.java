package com.app.service;

import com.app.entity.BrandEntity;
import com.app.entity.ContentThreadEntity;
import com.app.repository.ContentQuestionStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Dynamic Content Question Engine Tests")
class DynamicContentQuestionEngineTest {

    @Mock
    private ContentQuestionStateRepository questionStateRepository;

    @InjectMocks
    private DynamicContentQuestionEngine questionEngine;

    private BrandEntity brand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        brand = BrandEntity.builder()
            .name("Alpha Brands")
            .targetAudience("Startup Founders")
            .brandVoice("Conversational + Authoritative")
            .build();
    }

    @Test
    @DisplayName("Should generate content-type specific questions for LinkedIn post")
    void testLinkedInQuestions() {
        List<DynamicContentQuestionEngine.QuestionDefinition> questions = questionEngine.getQuestionsForContentType("LINKEDIN_POST", brand);

        assertFalse(questions.isEmpty());
        assertEquals("topic", questions.get(0).getKey());

        // Verify options derived from brand
        DynamicContentQuestionEngine.QuestionDefinition audienceQ = questions.stream()
            .filter(q -> "targetAudience".equals(q.getKey()))
            .findFirst()
            .orElseThrow();

        assertTrue(audienceQ.getDefaultOptions().stream().anyMatch(opt -> opt.contains("Startup Founders")));
    }

    @Test
    @DisplayName("Should save stateful question answers to repository")
    void testSaveAnswers() {
        ContentThreadEntity thread = ContentThreadEntity.builder().id(5L).build();
        Map<String, Map<String, String>> answers = Map.of(
            "topic", Map.of("title", "AI Automation", "selectedOption", "Other", "customValue", "AI Workflow Scaling")
        );

        when(questionStateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var saved = questionEngine.saveAnswers(thread, answers);

        assertEquals(1, saved.size());
        assertEquals("topic", saved.get(0).getQuestionKey());
        assertEquals("AI Workflow Scaling", saved.get(0).getCustomValue());
    }
}
