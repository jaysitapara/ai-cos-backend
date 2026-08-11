package com.app.service;

import com.app.model.cloud.CloudEnvironmentModel;
import com.app.model.cloud.InfrastructureTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CloudInfrastructureService {

    private final Map<String, CloudEnvironmentModel> envStore = new ConcurrentHashMap<>();
    private final Map<String, InfrastructureTemplate> templateStore = new ConcurrentHashMap<>();

    public CloudEnvironmentModel createEnvironment(String name, String type, String provider, String region) {
        String envId = "env-" + UUID.randomUUID();

        CloudEnvironmentModel env = CloudEnvironmentModel.builder()
            .environmentId(envId)
            .name(name != null ? name : "staging-env")
            .type(type != null ? type : "STAGING")
            .provider(provider != null ? provider : "AWS")
            .region(region != null ? region : "us-east-1")
            .status("READY")
            .configVersion(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        envStore.put(envId, env);

        InfrastructureTemplate template = InfrastructureTemplate.builder()
            .templateId("tmpl-" + UUID.randomUUID())
            .name("Spring Boot + React + PostgreSQL Infrastructure")
            .provider(env.getProvider())
            .services(Map.of(
                "application", "AppBackend (Java 17)",
                "database", "RDS PostgreSQL 15",
                "cache", "ElastiCache Redis",
                "frontend", "Nginx SPA Static Host"
            ))
            .secretsConfig(List.of("DB_PASSWORD", "JWT_SECRET"))
            .iacCode("""
                # Docker Compose IaC Blueprint
                version: '3.8'
                services:
                  backend:
                    image: app-backend:latest
                    ports:
                      - "8080:8080"
                  frontend:
                    image: app-frontend:latest
                    ports:
                      - "80:80"
                """)
            .build();

        templateStore.put(envId, template);
        return env;
    }

    public Optional<CloudEnvironmentModel> getEnvironment(String envId) {
        return Optional.ofNullable(envStore.get(envId));
    }

    public Optional<InfrastructureTemplate> getTemplate(String envId) {
        return Optional.ofNullable(templateStore.get(envId));
    }
}
