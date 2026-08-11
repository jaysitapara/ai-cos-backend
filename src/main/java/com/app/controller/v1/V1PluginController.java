package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.plugin.PluginModel;
import com.app.service.PluginEcosystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/plugins")
@RequiredArgsConstructor
@Tag(name = "V1 Plugin & Extension Ecosystem Engine", description = "Plugin Marketplace & Skill Extensions REST APIs")
public class V1PluginController {

    private final PluginEcosystemService pluginEcosystemService;

    @GetMapping
    @Operation(summary = "List installed plugins and extension capabilities")
    public ResponseEntity<List<PluginModel>> getInstalledPlugins() {
        return ResponseEntity.ok(pluginEcosystemService.getInstalledPlugins());
    }

    @PostMapping("/install")
    @Operation(summary = "Install plugin from marketplace catalog")
    public ResponseEntity<PluginModel> installPlugin(@RequestBody Map<String, String> body) {
        String pluginId = body.getOrDefault("pluginId", "plg-custom");
        String name = body.getOrDefault("name", "Custom Integration Plugin");
        String category = body.getOrDefault("category", "DEVELOPMENT");
        return ResponseEntity.ok(pluginEcosystemService.installPlugin(pluginId, name, category));
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable an installed plugin")
    public ResponseEntity<PluginModel> togglePlugin(@PathVariable String id) {
        return ResponseEntity.ok(pluginEcosystemService.togglePluginStatus(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Uninstall a plugin")
    public ResponseEntity<Void> uninstallPlugin(@PathVariable String id) {
        boolean removed = pluginEcosystemService.uninstallPlugin(id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/marketplace")
    @Operation(summary = "Browse plugin marketplace catalog")
    public ResponseEntity<List<PluginModel>> getMarketplaceCatalog() {
        return ResponseEntity.ok(pluginEcosystemService.getMarketplaceCatalog());
    }
}
