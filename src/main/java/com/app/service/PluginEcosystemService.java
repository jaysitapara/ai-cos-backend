package com.app.service;

import com.app.enums.PluginStatus;
import com.app.model.plugin.PluginModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginEcosystemService {

    private final Map<String, PluginModel> pluginStore = new ConcurrentHashMap<>();

    public List<PluginModel> getInstalledPlugins() {
        if (pluginStore.isEmpty()) {
            PluginModel p1 = PluginModel.builder()
                .pluginId("plg-github")
                .name("GitHub Actions Integration")
                .version("1.2.0")
                .publisher("AI-COS Official")
                .description("Autonomous repository management and CI workflow triggering")
                .category("DEVELOPMENT")
                .status(PluginStatus.ACTIVE)
                .permissions(List.of("REPO_READ", "REPO_WRITE", "PIPELINE_TRIGGER"))
                .installedAt(Instant.now())
                .build();

            PluginModel p2 = PluginModel.builder()
                .pluginId("plg-aws")
                .name("AWS Provisioning Adapter")
                .version("2.0.1")
                .publisher("Cloud Solutions")
                .description("Cloud Infrastructure IaC & ECS deployment integration")
                .category("CLOUD")
                .status(PluginStatus.ACTIVE)
                .permissions(List.of("AWS_READ", "AWS_PROVISION"))
                .installedAt(Instant.now())
                .build();

            pluginStore.put("plg-github", p1);
            pluginStore.put("plg-aws", p2);
        }
        return new ArrayList<>(pluginStore.values());
    }

    public PluginModel installPlugin(String pluginId, String name, String category) {
        PluginModel p = PluginModel.builder()
            .pluginId(pluginId)
            .name(name != null ? name : "Custom Extension Plugin")
            .version("1.0.0")
            .publisher("Third-Party Publisher")
            .description("Extended capabilities for AI-COS Operating System")
            .category(category != null ? category : "DEVELOPMENT")
            .status(PluginStatus.ACTIVE)
            .permissions(List.of("BASIC_READ"))
            .installedAt(Instant.now())
            .build();

        pluginStore.put(pluginId, p);
        return p;
    }

    public PluginModel togglePluginStatus(String pluginId) {
        PluginModel p = pluginStore.get(pluginId);
        if (p != null) {
            p.setStatus(p.getStatus() == PluginStatus.ACTIVE ? PluginStatus.DISABLED : PluginStatus.ACTIVE);
        }
        return p;
    }

    public boolean uninstallPlugin(String pluginId) {
        return pluginStore.remove(pluginId) != null;
    }

    public List<PluginModel> getMarketplaceCatalog() {
        return List.of(
            PluginModel.builder().pluginId("plg-slack").name("Slack Notifications").version("1.0.0").publisher("Integrations Co").description("Real-time alert routing to Slack channels").category("COMMUNICATION").status(PluginStatus.INSTALLED).build(),
            PluginModel.builder().pluginId("plg-jira").name("Jira User Story Sync").version("1.1.0").publisher("Atlassian Ecosystem").description("Sync generated user stories directly to Jira backlogs").category("BUSINESS").status(PluginStatus.INSTALLED).build()
        );
    }
}
