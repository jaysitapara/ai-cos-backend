package com.app.service;

import com.app.model.cloud.CloudEnvironmentModel;
import com.app.model.cloud.InfrastructureTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CloudInfrastructureServiceTest {

    private CloudInfrastructureService service;

    @BeforeEach
    void setUp() {
        service = new CloudInfrastructureService();
    }

    @Test
    void testCreateEnvironmentAndTemplate() {
        CloudEnvironmentModel env = service.createEnvironment("prod-us", "PRODUCTION", "AWS", "us-west-2");

        assertNotNull(env);
        assertEquals("prod-us", env.getName());
        assertEquals("PRODUCTION", env.getType());
        assertEquals("AWS", env.getProvider());

        Optional<InfrastructureTemplate> tmplOpt = service.getTemplate(env.getEnvironmentId());
        assertTrue(tmplOpt.isPresent());
        InfrastructureTemplate tmpl = tmplOpt.get();
        assertEquals("AWS", tmpl.getProvider());
        assertTrue(tmpl.getIacCode().contains("Docker Compose"));
    }
}
