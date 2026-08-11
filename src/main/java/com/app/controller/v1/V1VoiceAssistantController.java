package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.voice.VoiceSessionModel;
import com.app.service.ContinuousVoiceAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/voice-assistant")
@RequiredArgsConstructor
@Tag(name = "V1 Voice Assistant Engine", description = "Continuous Hands-Free Voice Workspace REST APIs")
public class V1VoiceAssistantController {

    private final ContinuousVoiceAssistantService voiceAssistantService;

    @PostMapping("/sessions/start")
    @Operation(summary = "Start continuous voice assistant session")
    public ResponseEntity<VoiceSessionModel> startSession(@RequestBody Map<String, String> body) {
        String userId = body.getOrDefault("userId", "user-default");
        String language = body.getOrDefault("language", "en-US");
        return ResponseEntity.ok(voiceAssistantService.startSession(userId, language));
    }

    @GetMapping("/sessions/{id}/status")
    @Operation(summary = "Get voice assistant session status and audio levels")
    public ResponseEntity<VoiceSessionModel> getStatus(@PathVariable String id) {
        return voiceAssistantService.getSession(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{id}/stop")
    @Operation(summary = "Stop active voice assistant session")
    public ResponseEntity<VoiceSessionModel> stopSession(@PathVariable String id) {
        return ResponseEntity.ok(voiceAssistantService.stopSession(id));
    }
}
