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
public class BranchModel {
    private String branchName;
    private String repositoryId;
    private boolean isProtected;
    private String lastCommitHash;
    private Instant createdAt;
}
