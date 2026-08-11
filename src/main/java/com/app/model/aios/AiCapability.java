package com.app.model.aios;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCapability {
    private String capabilityId;
    private String name;
    private String category; // AUTONOMOUS_DEV, MULTI_AGENT, VOICE, MEMORY, DEPLOYMENT
    private String description;
    private boolean isEnabled;
}
