package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.enums.MemoryCategory;
import com.app.model.memory.MemoryItemModel;
import com.app.service.LongTermMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/memory")
@RequiredArgsConstructor
@Tag(name = "V1 Long-Term AI Memory Engine", description = "Long-Term Memory & Personal AI REST APIs")
public class V1MemoryController {

    private final LongTermMemoryService memoryService;

    @PostMapping
    @Operation(summary = "Save new persistent memory item")
    public ResponseEntity<MemoryItemModel> saveMemory(@RequestBody Map<String, Object> body) {
        String userId = (String) body.getOrDefault("userId", "user-default");
        String categoryStr = (String) body.getOrDefault("category", "PREFERENCE");
        String title = (String) body.getOrDefault("title", "Preferred Architecture");
        String content = (String) body.getOrDefault("content", "Prefers Clean Architecture and SOLID design principles.");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.getOrDefault("tags", List.of("architecture"));

        MemoryCategory category = MemoryCategory.valueOf(categoryStr);
        return ResponseEntity.ok(memoryService.saveMemory(userId, category, title, content, tags));
    }

    @GetMapping
    @Operation(summary = "Search persistent memories by category")
    public ResponseEntity<List<MemoryItemModel>> searchMemories(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String category) {
        MemoryCategory cat = category != null ? MemoryCategory.valueOf(category) : null;
        return ResponseEntity.ok(memoryService.searchMemories(userId, cat));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete memory item by ID")
    public ResponseEntity<Void> deleteMemory(@PathVariable String id) {
        boolean deleted = memoryService.deleteMemory(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/context")
    @Operation(summary = "Get user personal AI active context")
    public ResponseEntity<Map<String, Object>> getUserContext(@RequestParam(required = false, defaultValue = "user-default") String userId) {
        return ResponseEntity.ok(memoryService.getUserContext(userId));
    }
}
