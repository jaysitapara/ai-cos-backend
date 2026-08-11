package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.runtime.RuntimeContextModel;
import com.app.service.ExecutionRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/runtime")
@RequiredArgsConstructor
@Tag(name = "V1 Execution Runtime Engine", description = "Autonomous Execution Runtime REST APIs")
public class V1RuntimeController {

    private final ExecutionRuntimeService executionRuntimeService;

    @PostMapping("/start")
    @Operation(summary = "Start runtime execution session for an approved plan")
    public ResponseEntity<RuntimeContextModel> startRuntime(@RequestBody Map<String, String> body) {
        String planId = body.getOrDefault("planId", "plan-default");
        return ResponseEntity.ok(executionRuntimeService.startRuntime(planId));
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get runtime execution status and progress")
    public ResponseEntity<RuntimeContextModel> getRuntime(@PathVariable String id) {
        return executionRuntimeService.getRuntime(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{id}/pause")
    @Operation(summary = "Pause runtime execution session")
    public ResponseEntity<RuntimeContextModel> pauseRuntime(@PathVariable String id) {
        return ResponseEntity.ok(executionRuntimeService.pauseRuntime(id));
    }

    @PostMapping("/sessions/{id}/resume")
    @Operation(summary = "Resume runtime execution session")
    public ResponseEntity<RuntimeContextModel> resumeRuntime(@PathVariable String id) {
        return ResponseEntity.ok(executionRuntimeService.resumeRuntime(id));
    }

    @PostMapping("/sessions/{id}/cancel")
    @Operation(summary = "Cancel runtime execution session")
    public ResponseEntity<RuntimeContextModel> cancelRuntime(@PathVariable String id) {
        return ResponseEntity.ok(executionRuntimeService.cancelRuntime(id));
    }
}
