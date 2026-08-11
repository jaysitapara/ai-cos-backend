package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.learning.LearningMetricsSummary;
import com.app.model.learning.UserFeedbackModel;
import com.app.service.SelfLearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/learning")
@RequiredArgsConstructor
@Tag(name = "V1 Autonomous Self-Learning Engine", description = "Self-Learning, Feedback & Continuous Optimization REST APIs")
public class V1LearningController {

    private final SelfLearningService selfLearningService;

    @PostMapping("/feedback")
    @Operation(summary = "Submit user feedback and rating for continuous model optimization")
    public ResponseEntity<UserFeedbackModel> submitFeedback(@RequestBody Map<String, Object> body) {
        String userId = (String) body.getOrDefault("userId", "user-default");
        String projectId = (String) body.getOrDefault("projectId", "proj-default");
        String category = (String) body.getOrDefault("category", "CODE");
        int rating = body.get("rating") instanceof Number ? ((Number) body.get("rating")).intValue() : 5;
        String comments = (String) body.getOrDefault("comments", "Great code generation quality!");

        return ResponseEntity.ok(selfLearningService.submitFeedback(userId, projectId, category, rating, comments));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get learning metrics, recommendation acceptance rate, and quality scores")
    public ResponseEntity<LearningMetricsSummary> getLearningMetrics() {
        return ResponseEntity.ok(selfLearningService.getLearningMetrics());
    }

    @GetMapping("/patterns")
    @Operation(summary = "List auto-extracted best practice patterns")
    public ResponseEntity<List<String>> getExtractedPatterns() {
        return ResponseEntity.ok(selfLearningService.getExtractedPatterns());
    }
}
