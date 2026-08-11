package com.app.service;

import com.app.enums.BuildStatus;
import com.app.model.build.BuildJobModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BuildOrchestrationService {

    private final Map<String, BuildJobModel> buildStore = new ConcurrentHashMap<>();

    public BuildJobModel createBuild(String repositoryId, String branch) {
        String buildId = "bld-" + UUID.randomUUID();

        String pipelineYaml = """
            name: AI-COS Autonomous CI/CD Pipeline
            on:
              push:
                branches: [ main, feature/* ]
            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v3
                  - name: Set up JDK 17
                    uses: actions/setup-java@v3
                    with:
                      java-version: '17'
                  - name: Build & Test
                    run: ./gradlew build test
            """;

        BuildJobModel build = BuildJobModel.builder()
            .buildId(buildId)
            .executionId(UUID.randomUUID())
            .repositoryId(repositoryId)
            .branch(branch != null ? branch : "main")
            .commitHash("c0ff33a1b2c3")
            .status(BuildStatus.SUCCESS)
            .triggerSource("AUTONOMOUS_ORCHESTRATOR")
            .durationMs(8500)
            .artifacts(List.of("app-backend.jar", "frontend-dist.zip"))
            .pipelineYaml(pipelineYaml)
            .createdAt(Instant.now())
            .build();

        buildStore.put(buildId, build);
        return build;
    }

    public Optional<BuildJobModel> getBuild(String buildId) {
        return Optional.ofNullable(buildStore.get(buildId));
    }
}
