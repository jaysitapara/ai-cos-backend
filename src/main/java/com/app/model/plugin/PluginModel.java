package com.app.model.plugin;

import com.app.enums.PluginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginModel {
    private String pluginId;
    private String name;
    private String version;
    private String publisher;
    private String description;
    private String category; // DEVELOPMENT, CLOUD, VOICE, MEMORY, ANALYTICS
    private PluginStatus status;
    private List<String> permissions;
    private Instant installedAt;
}
