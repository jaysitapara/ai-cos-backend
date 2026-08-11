package com.app.model.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedbackModel {
    private String feedbackId;
    private String userId;
    private String projectId;
    private String source;
    private String category; // PLANNING, ARCHITECTURE, CODE, UI, DEPLOYMENT
    private int rating; // 1 to 5
    private String comments;
    private String status; // RECEIVED, PROCESSED
    private Instant createdAt;
}
