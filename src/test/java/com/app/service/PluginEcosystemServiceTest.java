package com.app.service;

import com.app.enums.PluginStatus;
import com.app.model.plugin.PluginModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginEcosystemServiceTest {

    private PluginEcosystemService service;

    @BeforeEach
    void setUp() {
        service = new PluginEcosystemService();
    }

    @Test
    void testPluginLifecycleAndMarketplace() {
        List<PluginModel> installed = service.getInstalledPlugins();
        assertFalse(installed.isEmpty());

        PluginModel newPlg = service.installPlugin("plg-docker", "Docker Container Adapter", "CLOUD");
        assertNotNull(newPlg);
        assertEquals(PluginStatus.ACTIVE, newPlg.getStatus());

        PluginModel toggled = service.togglePluginStatus("plg-docker");
        assertEquals(PluginStatus.DISABLED, toggled.getStatus());

        boolean uninstalled = service.uninstallPlugin("plg-docker");
        assertTrue(uninstalled);

        List<PluginModel> catalog = service.getMarketplaceCatalog();
        assertFalse(catalog.isEmpty());
    }
}
