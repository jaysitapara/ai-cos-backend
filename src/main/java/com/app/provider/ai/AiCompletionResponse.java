package com.app.provider.ai;

public class AiCompletionResponse {
    private String content;
    private String modelName;
    private String finishReason;
    private long promptTokens;
    private long completionTokens;

    public AiCompletionResponse() {}

    public AiCompletionResponse(String content, String modelName, String finishReason, long promptTokens, long completionTokens) {
        this.content = content;
        this.modelName = modelName;
        this.finishReason = finishReason;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public long getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }
}
