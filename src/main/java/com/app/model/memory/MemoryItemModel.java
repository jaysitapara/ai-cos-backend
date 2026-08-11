package com.app.model.memory;

import com.app.enums.MemoryCategory;
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
public class MemoryItemModel {
    private String memoryId;
    private String userId;
    private MemoryCategory category;
    private String title;
    private String content;
    private double confidenceScore;
    private double importanceScore;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
