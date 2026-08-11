package com.app.model.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureTemplate {
    private String templateId;
    private String name;
    private String provider;
    private Map<String, Object> services;
    private List<String> secretsConfig;
    private String iacCode; // Terraform / Docker Compose template
}
