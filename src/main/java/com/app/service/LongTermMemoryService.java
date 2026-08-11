package com.app.service;

import com.app.enums.MemoryCategory;
import com.app.model.memory.MemoryItemModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LongTermMemoryService {

    private final Map<String, MemoryItemModel> memoryStore = new ConcurrentHashMap<>();

    public MemoryItemModel saveMemory(String userId, MemoryCategory category, String title, String content, List<String> tags) {
        String memId = "mem-" + UUID.randomUUID();

        MemoryItemModel item = MemoryItemModel.builder()
            .memoryId(memId)
            .userId(userId != null ? userId : "user-default")
            .category(category != null ? category : MemoryCategory.PREFERENCE)
            .title(title)
            .content(content)
            .confidenceScore(0.95)
            .importanceScore(0.90)
            .tags(tags != null ? tags : List.of("preference", "architecture"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        memoryStore.put(memId, item);
        return item;
    }

    public List<MemoryItemModel> searchMemories(String userId, MemoryCategory category) {
        return memoryStore.values().stream()
            .filter(m -> userId == null || m.getUserId().equals(userId))
            .filter(m -> category == null || m.getCategory() == category)
            .toList();
    }

    public boolean deleteMemory(String memoryId) {
        return memoryStore.remove(memoryId) != null;
    }

    public Map<String, Object> getUserContext(String userId) {
        return Map.of(
            "userId", userId != null ? userId : "user-default",
            "preferredStack", "Java 17 Spring Boot + React TypeScript",
            "codingStyle", "Clean Architecture & SOLID",
            "rememberedMemoriesCount", memoryStore.size()
        );
    }
}
