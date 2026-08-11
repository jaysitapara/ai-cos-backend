package com.app.model.frontend;

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
public class UiComponentSpec {
    private String componentId;
    private String name;
    private String purpose;
    private Map<String, String> props;
    private List<String> state;
    private List<String> events;
    private List<String> dependencies;
    private List<String> accessibilityRules;
}
