package com.app.controller.v1;

import com.app.common.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/conversations")
@RequiredArgsConstructor
@Tag(name = "V1 Conversations", description = "Conversation Engine REST APIs")
public class V1ConversationController {

    @GetMapping
    @Operation(summary = "List conversations with pagination, sorting and filtering")
    public ResponseEntity<Map<String, Object>> listConversations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "pinned", required = false) Boolean pinned) {
        return ResponseEntity.ok(Map.of(
            "content", List.of(),
            "page", page,
            "size", size,
            "totalElements", 0
        ));
    }

    @PostMapping
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<Map<String, Object>> createConversation(@RequestBody Map<String, Object> request) {
        String id = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
            "id", id,
            "title", request.getOrDefault("title", "New Conversation"),
            "isPinned", false,
            "isArchived", false,
            "createdAt", System.currentTimeMillis()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
            "id", id,
            "title", "Sample Conversation",
            "isPinned", false,
            "isArchived", false
        ));
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "Rename conversation")
    public ResponseEntity<Map<String, Object>> renameConversation(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("id", id, "title", body.getOrDefault("title", "Renamed Conversation")));
    }

    @PatchMapping("/{id}/pin")
    @Operation(summary = "Pin or unpin conversation")
    public ResponseEntity<Map<String, Object>> pinConversation(@PathVariable String id, @RequestParam boolean pinned) {
        return ResponseEntity.ok(Map.of("id", id, "isPinned", pinned));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete conversation by ID")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }
}
