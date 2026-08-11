package com.app.model.frontend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendPlan {
    private String planId;
    private String blueprintId;
    private List<String> routeStructure;
    private Map<String, Object> designSystemTokens;
    private List<UiComponentSpec> components;
    private Instant createdAt;
}
