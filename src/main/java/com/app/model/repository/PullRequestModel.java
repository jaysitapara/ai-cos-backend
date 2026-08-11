package com.app.model.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestModel {
    private String prId;
    private String repositoryId;
    private String title;
    private String description;
    private String sourceBranch;
    private String targetBranch;
    private String status; // OPEN, MERGED, CLOSED
    private Instant createdAt;
}
