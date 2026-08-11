package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.crossproject.CrossProjectRecommendation;
import com.app.model.crossproject.ProjectRelationshipGraph;
import com.app.service.CrossProjectIntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/cross-project")
@RequiredArgsConstructor
@Tag(name = "V1 Cross-Project Intelligence Engine", description = "Cross-Project Knowledge & Pattern Reuse REST APIs")
public class V1CrossProjectController {

    private final CrossProjectIntelligenceService crossProjectIntelligenceService;

    @GetMapping("/recommendations")
    @Operation(summary = "Get intelligent recommendations for reusable modules and patterns")
    public ResponseEntity<List<CrossProjectRecommendation>> getRecommendations(@RequestParam(required = false, defaultValue = "proj-current") String projectId) {
        return ResponseEntity.ok(crossProjectIntelligenceService.getRecommendations(projectId));
    }

    @GetMapping("/relationships/{projectId}")
    @Operation(summary = "Get project relationship graph and shared assets summary")
    public ResponseEntity<ProjectRelationshipGraph> getRelationshipGraph(@PathVariable String projectId) {
        return ResponseEntity.ok(crossProjectIntelligenceService.getRelationshipGraph(projectId));
    }
}
