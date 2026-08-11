package com.app.provider.ai;

import java.util.Map;

public class AiCompletionRequest {
    private String prompt;
    private String systemInstruction;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, Object> parameters;

    public AiCompletionRequest() {}

    public AiCompletionRequest(String prompt, String systemInstruction, String model, Double temperature, Integer maxTokens, Map<String, Object> parameters) {
        this.prompt = prompt;
        this.systemInstruction = systemInstruction;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.parameters = parameters;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(String systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
