package com.app.model.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudEnvironmentModel {
    private String environmentId;
    private String name;
    private String type; // DEVELOPMENT, QA, STAGING, PRODUCTION
    private String provider; // AWS, AZURE, GCP, VERCEL, KUBERNETES
    private String region;
    private String status; // PROVISIONED, READY, TERMINATED
    private int configVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
