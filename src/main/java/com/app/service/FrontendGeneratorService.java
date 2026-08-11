package com.app.service;

import com.app.model.frontend.FrontendPlan;
import com.app.model.frontend.UiComponentSpec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FrontendGeneratorService {

    private final Map<String, FrontendPlan> planStore = new ConcurrentHashMap<>();

    public FrontendPlan generateFrontendPlan(String blueprintId) {
        String planId = "fe-" + UUID.randomUUID();

        UiComponentSpec header = UiComponentSpec.builder()
            .componentId("cmp-header")
            .name("UserHeader")
            .purpose("Global navigation bar with command palette trigger and user profile menu")
            .props(Map.of("user", "User", "onSearchClick", "() => void"))
            .state(List.of("isProfileOpen"))
            .events(List.of("onSearchClick", "onLogout"))
            .dependencies(List.of("LucideIcons"))
            .accessibilityRules(List.of("nav element with role=navigation", "aria-label=User Header"))
            .build();

        UiComponentSpec workspace = UiComponentSpec.builder()
            .componentId("cmp-workspace")
            .name("WorkspacePage")
            .purpose("3-column responsive AI Workspace with prompt composer and context panel")
            .props(Map.of("sessionId", "String"))
            .state(List.of("isRightPanelOpen", "promptText"))
            .events(List.of("onSendPrompt", "onFileUpload"))
            .dependencies(List.of("ConversationArea", "PromptArea", "AssumptionsArea"))
            .accessibilityRules(List.of("main role=main"))
            .build();

        FrontendPlan plan = FrontendPlan.builder()
            .planId(planId)
            .blueprintId(blueprintId)
            .routeStructure(List.of("/", "/app/workspace", "/app/jobs", "/app/outputs", "/app/history", "/app/files", "/app/settings"))
            .designSystemTokens(Map.of(
                "primaryColor", "#0f172a",
                "accentColor", "#2563eb",
                "fontFamily", "Inter, sans-serif",
                "borderRadius", "8px"
            ))
            .components(List.of(header, workspace))
            .createdAt(Instant.now())
            .build();

        planStore.put(planId, plan);
        return plan;
    }

    public Optional<FrontendPlan> getPlan(String planId) {
        return Optional.ofNullable(planStore.get(planId));
    }
}
