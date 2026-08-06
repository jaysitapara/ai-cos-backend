package com.app.service.impl;

import com.app.service.HealthService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public Map<String, String> getHealthStatus() {
        return Map.of(
            "status", "UP",
            "service", "app-backend"
        );
    }
}
