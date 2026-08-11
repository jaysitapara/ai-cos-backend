package com.app.model.repository;

import com.app.enums.GitProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryModel {
    private String repositoryId;
    private String name;
    private GitProviderType provider;
    private String defaultBranch;
    private String visibility; // PUBLIC, PRIVATE
    private String remoteUrl;
    private String status; // INITIALIZED, READY, ARCHIVED
    private Instant createdAt;
    private Instant updatedAt;
}
