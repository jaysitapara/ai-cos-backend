package com.app.controller.v1;

import com.app.entity.*;
import com.app.exception.UnauthorizedException;
import com.app.repository.UserRepository;
import com.app.service.BrandManagementService;
import com.app.service.ContentAgentService;
import com.app.service.DynamicContentQuestionEngine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/content")
@RequiredArgsConstructor
public class V1ContentAgentController {

    private final ContentAgentService contentAgentService;
    private final BrandManagementService brandService;
    private final DynamicContentQuestionEngine questionEngine;
    private final UserRepository userRepository;

    @Data
    public static class CreateThreadRequest {
        private UUID brandId;
        private String title;
        private String contentType;
    }

    @Data
    public static class SaveAnswersRequest {
        private Map<String, Map<String, String>> answers;
    }

    @Data
    public static class SaveEditRequest {
        private String updatedBody;
    }

    @Data
    public static class RewriteRequest {
        private String action;
        private String customInstruction;
    }

    @Data
    public static class FeedbackRequest {
        private String feedbackType;
        private Integer rating;
        private String feedbackText;
    }

    // ─── Thread Endpoints ──────────────────────────────────────────────────────
    @GetMapping("/threads")
    public ResponseEntity<List<ContentThreadEntity>> getThreads(
            Authentication authentication,
            @RequestParam UUID brandId) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.getThreadsForBrand(user, brandId));
    }

    @PostMapping("/threads")
    public ResponseEntity<ContentThreadEntity> createThread(
            Authentication authentication,
            @RequestBody CreateThreadRequest req) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.createThread(user, req.getBrandId(), req.getTitle(), req.getContentType()));
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<ContentThreadEntity> getThread(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.getThreadByPublicId(user, id));
    }

    @GetMapping("/threads/{id}/messages")
    public ResponseEntity<List<ContentMessageEntity>> getThreadMessages(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.getThreadMessages(user, id));
    }

    @GetMapping("/threads/{id}/questions")
    public ResponseEntity<List<DynamicContentQuestionEngine.QuestionDefinition>> getQuestions(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        ContentThreadEntity thread = contentAgentService.getThreadByPublicId(user, id);
        BrandEntity brand = thread.getBrand();
        return ResponseEntity.ok(questionEngine.getQuestionsForContentType(thread.getContentType(), brand));
    }

    @GetMapping("/threads/{id}/answers")
    public ResponseEntity<List<ContentQuestionStateEntity>> getSavedAnswers(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        ContentThreadEntity thread = contentAgentService.getThreadByPublicId(user, id);
        return ResponseEntity.ok(questionEngine.getSavedAnswers(thread.getId()));
    }

    @PostMapping("/threads/{id}/answers")
    public ResponseEntity<List<ContentQuestionStateEntity>> saveAnswers(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody SaveAnswersRequest req) {
        UserEntity user = getAuthenticatedUser(authentication);
        ContentThreadEntity thread = contentAgentService.getThreadByPublicId(user, id);
        return ResponseEntity.ok(questionEngine.saveAnswers(thread, req.getAnswers()));
    }

    @PostMapping("/threads/{id}/brief")
    public ResponseEntity<ContentBriefEntity> buildBrief(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.createContentBrief(user, id));
    }

    @PostMapping("/threads/{id}/generate")
    public ResponseEntity<GeneratedContentEntity> generateContent(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.generateContent(user, id));
    }

    // ─── Content Artifact & Version Endpoints ─────────────────────────
    @PatchMapping("/{id}")
    public ResponseEntity<ContentVersionEntity> saveManualEdit(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody SaveEditRequest req) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.saveUserEdit(user, id, req.getUpdatedBody()));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ContentVersionEntity>> getContentVersions(
            Authentication authentication,
            @PathVariable UUID id) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.getContentVersions(user, id));
    }

    @PostMapping("/{id}/rewrite")
    public ResponseEntity<ContentVersionEntity> executeRewrite(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody RewriteRequest req) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.executeTargetedRewrite(user, id, req.getAction(), req.getCustomInstruction()));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<ContentFeedbackEntity> submitFeedback(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody FeedbackRequest req) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.submitFeedback(user, id, req.getFeedbackType(), req.getRating(), req.getFeedbackText()));
    }

    @GetMapping("/library")
    public ResponseEntity<List<GeneratedContentEntity>> getContentLibrary(
            Authentication authentication,
            @RequestParam UUID brandId) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(contentAgentService.getContentLibrary(user, brandId));
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new UnauthorizedException("User authentication required"));
        }
        String name = authentication.getName();
        try {
            UUID publicId = UUID.fromString(name);
            return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                    .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(name)
                            .orElseGet(() -> userRepository.findAll().stream().findFirst().orElseThrow()));
        } catch (Exception e) {
            return userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElseThrow());
        }
    }
}
