package com.app.controller.v1;

import com.app.common.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/jobs")
@RequiredArgsConstructor
@Tag(name = "V1 Jobs", description = "Workloads & Job Execution REST APIs")
public class V1JobController {

    @GetMapping
    @Operation(summary = "List jobs with filtering and status tabs")
    public ResponseEntity<List<Map<String, Object>>> listJobs(@RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job details by ID")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
            "id", id,
            "title", "Sample Execution Job",
            "status", "RUNNING",
            "progress", Map.of("percentage", 75, "completedSteps", 3, "totalSteps", 4)
        ));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel running job")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "CANCELLED"));
    }
}
