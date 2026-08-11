package com.app.controller.v1;

import com.app.common.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/settings")
@RequiredArgsConstructor
@Tag(name = "V1 User Settings", description = "User Preferences & Settings REST APIs")
public class V1UserSettingsController {

    @GetMapping
    @Operation(summary = "Get user platform settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(Map.of(
            "theme", "system",
            "notificationsEnabled", true,
            "defaultAiProvider", "OpenAI"
        ));
    }

    @PutMapping
    @Operation(summary = "Update user platform settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        return ResponseEntity.ok(settings);
    }
}
