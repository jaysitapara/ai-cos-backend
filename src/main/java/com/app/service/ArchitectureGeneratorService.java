package com.app.service;

import com.app.model.architecture.ApiEndpointSpec;
import com.app.model.architecture.EntitySchema;
import com.app.model.architecture.TechnicalBlueprint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ArchitectureGeneratorService {

    private final Map<String, TechnicalBlueprint> blueprintStore = new ConcurrentHashMap<>();

    public TechnicalBlueprint generateBlueprint(String specId) {
        String blueprintId = "blue-" + UUID.randomUUID();

        EntitySchema uEntity = EntitySchema.builder()
            .entityName("User")
            .tableName("app_users")
            .fields(List.of(
                Map.of("fieldName", "id", "type", "UUID", "primaryKey", true),
                Map.of("fieldName", "email", "type", "VARCHAR(255)", "nullable", false),
                Map.of("fieldName", "passwordHash", "type", "VARCHAR(255)", "nullable", false)
            ))
            .relationships(List.of())
            .build();

        ApiEndpointSpec ep1 = ApiEndpointSpec.builder()
            .httpMethod("POST")
            .path("/api/v1/auth/login")
            .summary("Authenticate user and return JWT tokens")
            .requestBody(Map.of("email", "String", "password", "String"))
            .responseBody(Map.of("token", "String", "user", "Object"))
            .authRequired(false)
            .build();

        TechnicalBlueprint blueprint = TechnicalBlueprint.builder()
            .blueprintId(blueprintId)
            .specId(specId)
            .systemArchitectureMarkdown("# System Architecture\n3-Tier Clean Architecture: Spring Boot API Backend + React Frontend + PostgreSQL DB.")
            .entities(List.of(uEntity))
            .apiEndpoints(List.of(ep1))
            .deploymentTopology("Docker Compose containerized deployment with Nginx frontend proxy.")
            .createdAt(Instant.now())
            .build();

        blueprintStore.put(blueprintId, blueprint);
        return blueprint;
    }

    public Optional<TechnicalBlueprint> getBlueprint(String blueprintId) {
        return Optional.ofNullable(blueprintStore.get(blueprintId));
    }
}
