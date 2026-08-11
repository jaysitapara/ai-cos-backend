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
@RequestMapping(ApiConstants.API_V1 + "/conversations/{conversationId}/messages")
@RequiredArgsConstructor
@Tag(name = "V1 Messages", description = "Message Engine REST APIs")
public class V1MessageController {

    @GetMapping
    @Operation(summary = "List messages in a conversation")
    public ResponseEntity<List<Map<String, Object>>> getMessages(@PathVariable String conversationId) {
        return ResponseEntity.ok(List.of(
            Map.of(
                "id", UUID.randomUUID().toString(),
                "conversationId", conversationId,
                "role", "USER",
                "contentType", "TEXT",
                "content", "Analyze supplier contract renewals",
                "createdAt", System.currentTimeMillis()
            )
        ));
    }

    @PostMapping
    @Operation(summary = "Send a new turn message")
    public ResponseEntity<Map<String, Object>> sendMessage(@PathVariable String conversationId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
            "id", UUID.randomUUID().toString(),
            "conversationId", conversationId,
            "role", body.getOrDefault("role", "USER"),
            "contentType", body.getOrDefault("contentType", "TEXT"),
            "content", body.getOrDefault("content", ""),
            "createdAt", System.currentTimeMillis()
        ));
    }
}
