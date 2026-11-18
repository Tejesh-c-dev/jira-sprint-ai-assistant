package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.SprintAnalysisResponse;
import com.ablmvp.sprintassistant.api.dto.SprintDetailResponse;
import com.ablmvp.sprintassistant.api.dto.SprintSummaryResponse;
import com.ablmvp.sprintassistant.api.dto.SprintHealthResponse;
import com.ablmvp.sprintassistant.api.dto.SprintComparisonResponse;
import com.ablmvp.sprintassistant.api.dto.TrendResponse;
import com.ablmvp.sprintassistant.metrics.SprintHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sprints")
@Tag(name = "Sprints")
public class SprintApiController {
    private final SprintReadService readService;
    private final SprintHealthService healthService;
    private final HistoricalSprintService historicalService;

    public SprintApiController(SprintReadService readService,
                                SprintHealthService healthService,
                                HistoricalSprintService historicalService) {
        this.readService = readService;
        this.healthService = healthService;
        this.historicalService = historicalService;
    }

    @GetMapping
    @Operation(summary = "List available sprints")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprints returned"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public List<SprintSummaryResponse> listSprints() {
        return readService.listSprints();
    }

    @GetMapping("/{sprintId}")
    @Operation(summary = "Get source details for a sprint")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint details returned"),
        @ApiResponse(responseCode = "400", description = "Invalid sprint identifier"),
        @ApiResponse(responseCode = "404", description = "Sprint not found"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<SprintDetailResponse> getSprint(@PathVariable String sprintId) {
        return ResponseEntity.ok(readService.getSprint(sprintId));
    }

    @GetMapping("/{sprintId}/analysis")
    @Operation(summary = "Get approved sprint source facts and membership metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint analysis returned"),
        @ApiResponse(responseCode = "400", description = "Invalid sprint identifier"),
        @ApiResponse(responseCode = "404", description = "Sprint not found"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<SprintAnalysisResponse> getAnalysis(@PathVariable String sprintId) {
        return ResponseEntity.ok(readService.analyzeSprint(sprintId));
    }

    @GetMapping("/{sprintId}/health")
    public ResponseEntity<SprintHealthResponse> getHealth(@PathVariable String sprintId) {
        readService.getSprint(sprintId);
        return ResponseEntity.ok(ApiMapper.toHealth(
                healthService.analyze(readService.loadResult(), sprintId)));
    }

    @GetMapping("/compare")
    public ResponseEntity<SprintComparisonResponse> compare(
            @RequestParam(name = "sprintIds", required = false) List<String> sprintIds) {
        return ResponseEntity.ok(historicalService.compare(sprintIds));
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendResponse> trends() {
        return ResponseEntity.ok(historicalService.trends());
    }
}
