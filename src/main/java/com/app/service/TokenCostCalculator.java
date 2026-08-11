package com.app.service;

import org.springframework.stereotype.Service;

@Service
public class TokenCostCalculator {

    /**
     * Calculates estimated cost in USD based on model name and token counts.
     * Supports Google Gemini and OpenAI model pricing structures.
     */
    public double calculateCost(String modelName, long promptTokens, long completionTokens) {
        if (modelName == null) modelName = "";
        String lower = modelName.toLowerCase();

        double promptRate = 0.0005 / 1000;      // Default $0.50 per 1M tokens
        double completionRate = 0.0015 / 1000;  // Default $1.50 per 1M tokens

        if (lower.contains("gemini-1.5-flash") || lower.contains("gemini-flash")) {
            promptRate = 0.000075 / 1000;
            completionRate = 0.00030 / 1000;
        } else if (lower.contains("gemini")) {
            promptRate = 0.00125 / 1000;
            completionRate = 0.00500 / 1000;
        } else if (lower.contains("gpt-4o-mini")) {
            promptRate = 0.00015 / 1000;
            completionRate = 0.00060 / 1000;
        } else if (lower.contains("gpt-4")) {
            promptRate = 0.0025 / 1000;
            completionRate = 0.0100 / 1000;
        } else if (lower.contains("gpt-3.5")) {
            promptRate = 0.0005 / 1000;
            completionRate = 0.0015 / 1000;
        }

        return (promptTokens * promptRate) + (completionTokens * completionRate);
    }
}
