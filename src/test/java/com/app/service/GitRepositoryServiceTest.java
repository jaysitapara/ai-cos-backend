package com.app.service;

import com.app.enums.GitProviderType;
import com.app.model.repository.BranchModel;
import com.app.model.repository.PullRequestModel;
import com.app.model.repository.RepositoryModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitRepositoryServiceTest {

    private GitRepositoryService service;

    @BeforeEach
    void setUp() {
        service = new GitRepositoryService();
    }

    @Test
    void testCreateRepoBranchAndPullRequest() {
        RepositoryModel repo = service.createRepository("ai-cos-core", GitProviderType.GITHUB);

        assertNotNull(repo);
        assertEquals("ai-cos-core", repo.getName());
        assertEquals("main", repo.getDefaultBranch());

        List<BranchModel> branches = service.listBranches(repo.getRepositoryId());
        assertEquals(1, branches.size());

        BranchModel featureBranch = service.createBranch(repo.getRepositoryId(), "feature/auth-service");
        assertNotNull(featureBranch);
        assertEquals("feature/auth-service", featureBranch.getBranchName());

        PullRequestModel pr = service.createPullRequest(repo.getRepositoryId(), "Add Auth Service", "Automated PR", "feature/auth-service", "main");
        assertNotNull(pr);
        assertEquals("OPEN", pr.getStatus());
    }
}
