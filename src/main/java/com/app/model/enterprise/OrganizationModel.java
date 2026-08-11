package com.app.model.enterprise;

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
public class OrganizationModel {
    private String orgId;
    private String name;
    private String planTier; // ENTERPRISE, BUSINESS, DEVELOPER
    private int teamsCount;
    private List<String> complianceFrameworks;
    private String status; // ACTIVE, SUSPENDED
    private Instant createdAt;
}