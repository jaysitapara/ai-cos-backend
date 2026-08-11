package com.app.service;

import com.app.entity.BrandEntity;
import com.app.entity.ContentQuestionStateEntity;
import com.app.entity.ContentThreadEntity;
import com.app.repository.ContentQuestionStateRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicContentQuestionEngine {

    private final ContentQuestionStateRepository questionStateRepository;

    @Data
    @Builder
    public static class QuestionDefinition {
        private String key;
        private String title;
        private String description;
        private String type; // SINGLE_SELECT, MULTI_SELECT, TEXT, TEXTAREA
        private boolean required;
        private boolean supportsOther;
        private List<String> defaultOptions;
    }

    public List<QuestionDefinition> getQuestionsForContentType(String contentType, BrandEntity brand) {
        String normalized = contentType != null ? contentType.toUpperCase() : "LINKEDIN_POST";

        List<QuestionDefinition> questions = new ArrayList<>();

        switch (normalized) {
            case "LINKEDIN_POST" -> {
                questions.add(QuestionDefinition.builder()
                    .key("topic")
                    .title("What topic or key takeaway do you want to cover?")
                    .description("The main idea or insight for this LinkedIn post.")
                    .type("TEXTAREA")
                    .required(true)
                    .supportsOther(false)
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("goal")
                    .title("What is the primary objective of this post?")
                    .description("Select what success looks like.")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of(
                        "Build Thought Leadership",
                        "Generate Inbound Leads",
                        "Educate Audience / Share How-To",
                        "Share Personal Story or Milestone"
                    ))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("targetAudience")
                    .title("Who is the primary audience?")
                    .description("Target reader persona.")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveAudienceOptions(brand))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("tone")
                    .title("What tone of voice should we use?")
                    .description("Style of communication.")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveToneOptions(brand))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("cta")
                    .title("Call to Action (CTA)")
                    .description("What action should readers take at the end?")
                    .type("SINGLE_SELECT")
                    .required(false)
                    .supportsOther(true)
                    .defaultOptions(List.of(
                        "Ask a question in comments",
                        "Link to website / product",
                        "Invite direct message (DM)",
                        "No explicit CTA (pure value)"
                    ))
                    .build());
            }
            case "INSTAGRAM_CAPTION" -> {
                questions.add(QuestionDefinition.builder()
                    .key("topic")
                    .title("What visual or concept does this caption accompany?")
                    .description("Describe the image/video context.")
                    .type("TEXTAREA")
                    .required(true)
                    .supportsOther(false)
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("goal")
                    .title("Content Objective")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of("Drive Engagement / Saves", "Product Launch / Promo", "Behind the Scenes", "Community Building"))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("tone")
                    .title("Caption Style")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveToneOptions(brand))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("cta")
                    .title("Call to Action")
                    .type("SINGLE_SELECT")
                    .required(false)
                    .supportsOther(true)
                    .defaultOptions(List.of("Link in bio", "Comment below", "Save for later", "Share with a friend"))
                    .build());
            }
            case "BLOG_ARTICLE" -> {
                questions.add(QuestionDefinition.builder()
                    .key("topic")
                    .title("Article Topic & Main Objective")
                    .type("TEXTAREA")
                    .required(true)
                    .supportsOther(false)
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("targetAudience")
                    .title("Target Reader Audience")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveAudienceOptions(brand))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("depth")
                    .title("Article Depth & Format")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of("Short Guide (500-800 words)", "In-Depth How-To (1200-1800 words)", "Case Study", "Opinion / Essay"))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("tone")
                    .title("Writing Tone")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveToneOptions(brand))
                    .build());
            }
            case "AD_COPY" -> {
                questions.add(QuestionDefinition.builder()
                    .key("topic")
                    .title("Product or Offer Being Advertised")
                    .type("TEXTAREA")
                    .required(true)
                    .supportsOther(false)
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("targetAudience")
                    .title("Target Customer Segment")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveAudienceOptions(brand))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("goal")
                    .title("Campaign Goal")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of("Direct Conversions / Sales", "Lead Generation", "Brand Awareness", "Event Signups"))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("cta")
                    .title("Primary CTA Button Text")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of("Get Started Free", "Book a Demo", "Shop Now", "Learn More"))
                    .build());
            }
            default -> {
                questions.add(QuestionDefinition.builder()
                    .key("topic")
                    .title("What content would you like to create?")
                    .type("TEXTAREA")
                    .required(true)
                    .supportsOther(false)
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("goal")
                    .title("Main Goal")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(List.of("Audience Growth", "Authority", "Conversion", "Engagement"))
                    .build());

                questions.add(QuestionDefinition.builder()
                    .key("tone")
                    .title("Desired Tone")
                    .type("SINGLE_SELECT")
                    .required(true)
                    .supportsOther(true)
                    .defaultOptions(deriveToneOptions(brand))
                    .build());
            }
        }

        return questions;
    }

    private List<String> deriveAudienceOptions(BrandEntity brand) {
        List<String> list = new ArrayList<>();
        if (brand != null && brand.getTargetAudience() != null && !brand.getTargetAudience().isBlank()) {
            list.add("[Brand Default] " + brand.getTargetAudience());
        }
        list.add("Founders & C-Suite Executives");
        list.add("Software Engineers & Tech Leaders");
        list.add("Marketing & Sales Professionals");
        list.add("General Industry Professionals");
        return list;
    }

    private List<String> deriveToneOptions(BrandEntity brand) {
        List<String> list = new ArrayList<>();
        if (brand != null && brand.getBrandVoice() != null && !brand.getBrandVoice().isBlank()) {
            list.add("[Brand Default] " + brand.getBrandVoice());
        }
        list.add("Authoritative + Professional");
        list.add("Conversational + Insightful");
        list.add("Direct + High Impact");
        list.add("Storyteller + Educational");
        return list;
    }

    @Transactional
    public List<ContentQuestionStateEntity> saveAnswers(ContentThreadEntity thread, Map<String, Map<String, String>> answersMap) {
        List<ContentQuestionStateEntity> savedStates = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Map<String, String>> entry : answersMap.entrySet()) {
            String key = entry.getKey();
            Map<String, String> data = entry.getValue();
            String title = data.getOrDefault("title", key);
            String selectedOption = data.get("selectedOption");
            String customValue = data.get("customValue");

            ContentQuestionStateEntity state = questionStateRepository.findByThreadIdAndQuestionKey(thread.getId(), key)
                .orElseGet(() -> ContentQuestionStateEntity.builder()
                    .thread(thread)
                    .questionKey(key)
                    .build());

            state.setQuestionTitle(title);
            state.setSelectedOption(selectedOption);
            state.setCustomValue(customValue);
            state.setOrderIndex(index++);
            savedStates.add(questionStateRepository.save(state));
        }
        return savedStates;
    }

    @Transactional(readOnly = true)
    public List<ContentQuestionStateEntity> getSavedAnswers(Long threadId) {
        return questionStateRepository.findByThreadIdOrderByOrderIndexAsc(threadId);
    }
}
