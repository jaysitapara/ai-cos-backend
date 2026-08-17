package com.app.service.impl;

import com.app.service.HealthService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HealthServiceImpl implements HealthService {

    private final javax.sql.DataSource dataSource;

    public HealthServiceImpl(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Map<String, String> getHealthStatus() {
        boolean dbHealthy = false;
        try (java.sql.Connection conn = dataSource.getConnection()) {
            dbHealthy = conn.isValid(2);
        } catch (Exception ignored) {}

        return Map.of(
            "status", dbHealthy ? "UP" : "DOWN",
            "database", dbHealthy ? "CONNECTED" : "DISCONNECTED",
            "service", "app-backend"
        );
    }
}
