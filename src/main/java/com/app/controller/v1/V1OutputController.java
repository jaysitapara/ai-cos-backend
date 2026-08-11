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
@RequestMapping(ApiConstants.API_V1 + "/outputs")
@RequiredArgsConstructor
@Tag(name = "V1 Outputs", description = "Artifact & Output Deliverable REST APIs")
public class V1OutputController {

    @GetMapping
    @Operation(summary = "List generated outputs")
    public ResponseEntity<List<Map<String, Object>>> listOutputs(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get output metadata by ID")
    public ResponseEntity<Map<String, Object>> getOutput(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
            "id", id,
            "name", "artifact_output.py",
            "contentType", "text/x-python",
            "sizeBytes", 14200
        ));
    }
}
