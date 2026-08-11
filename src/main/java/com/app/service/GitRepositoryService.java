package com.app.service;

import com.app.enums.GitProviderType;
import com.app.model.repository.BranchModel;
import com.app.model.repository.PullRequestModel;
import com.app.model.repository.RepositoryModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitRepositoryService {

    private final Map<String, RepositoryModel> repoStore = new ConcurrentHashMap<>();
    private final Map<String, List<BranchModel>> branchStore = new ConcurrentHashMap<>();
    private final Map<String, List<PullRequestModel>> prStore = new ConcurrentHashMap<>();

    public RepositoryModel createRepository(String name, GitProviderType provider) {
        String repoId = "repo-" + UUID.randomUUID();

        RepositoryModel repo = RepositoryModel.builder()
            .repositoryId(repoId)
            .name(name)
            .provider(provider != null ? provider : GitProviderType.GITHUB)
            .defaultBranch("main")
            .visibility("PRIVATE")
            .remoteUrl("https://github.com/org/" + name + ".git")
            .status("READY")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        repoStore.put(repoId, repo);

        BranchModel mainBranch = BranchModel.builder()
            .branchName("main")
            .repositoryId(repoId)
            .isProtected(true)
            .lastCommitHash("a1b2c3d4e5f6")
            .createdAt(Instant.now())
            .build();

        branchStore.put(repoId, Collections.synchronizedList(new ArrayList<>(List.of(mainBranch))));
        return repo;
    }

    public Optional<RepositoryModel> getRepository(String repoId) {
        return Optional.ofNullable(repoStore.get(repoId));
    }

    public List<BranchModel> listBranches(String repoId) {
        return branchStore.getOrDefault(repoId, List.of());
    }

    public BranchModel createBranch(String repoId, String branchName) {
        BranchModel branch = BranchModel.builder()
            .branchName(branchName)
            .repositoryId(repoId)
            .isProtected(false)
            .lastCommitHash("f6e5d4c3b2a1")
            .createdAt(Instant.now())
            .build();

        branchStore.computeIfAbsent(repoId, k -> Collections.synchronizedList(new ArrayList<>())).add(branch);
        return branch;
    }

    public PullRequestModel createPullRequest(String repoId, String title, String description, String sourceBranch, String targetBranch) {
        String prId = "pr-" + UUID.randomUUID();
        PullRequestModel pr = PullRequestModel.builder()
            .prId(prId)
            .repositoryId(repoId)
            .title(title)
            .description(description)
            .sourceBranch(sourceBranch)
            .targetBranch(targetBranch != null ? targetBranch : "main")
            .status("OPEN")
            .createdAt(Instant.now())
            .build();

        prStore.computeIfAbsent(repoId, k -> Collections.synchronizedList(new ArrayList<>())).add(pr);
        return pr;
    }
}
