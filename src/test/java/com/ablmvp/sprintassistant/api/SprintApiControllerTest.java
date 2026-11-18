package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.SprintDetailResponse;
import com.ablmvp.sprintassistant.api.dto.SprintSummaryResponse;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import com.ablmvp.sprintassistant.metrics.MetricAvailability;
import com.ablmvp.sprintassistant.metrics.MetricResult;
import com.ablmvp.sprintassistant.metrics.SprintMetricsResult;
import com.ablmvp.sprintassistant.metrics.SprintSourceFacts;
import com.ablmvp.sprintassistant.metrics.SprintHealthService;
import com.ablmvp.sprintassistant.metrics.SprintHealthResult;
import com.ablmvp.sprintassistant.metrics.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.ResponseEntity;
import com.ablmvp.sprintassistant.api.dto.ErrorResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SprintApiController.class)
class SprintApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SprintReadService readService;

    @MockBean
    private SprintHealthService healthService;

    @MockBean
    private HistoricalSprintService historicalService;

    @Test
    void listsMultipleSprintsInServiceOrder() throws Exception {
        when(readService.listSprints()).thenReturn(List.of(
                new SprintSummaryResponse("S-1", "Alpha", "CLOSED", null, null, null, 5),
                new SprintSummaryResponse("S-2", "Beta", "CLOSED", null, null, null, 10)));

        mockMvc.perform(get("/api/v1/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sprintId").value("S-1"))
                .andExpect(jsonPath("$[1].sprintId").value("S-2"));
    }

    @Test
    void returnsEmptySprintList() throws Exception {
        when(readService.listSprints()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getsSprintDetails() throws Exception {
        when(readService.getSprint("S-1")).thenReturn(new SprintDetailResponse(
                "S-1", "Alpha", "CLOSED", null, null, null, 5, 3,
                12, 8, 2, 1, 1, List.of("I-3"), null));

        mockMvc.perform(get("/api/v1/sprints/S-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value("S-1"))
                .andExpect(jsonPath("$.recordedIssueCount").value(12));
    }

    @Test
    void returns404ForUnknownSprint() throws Exception {
        when(readService.getSprint("missing")).thenThrow(new SprintNotFoundException("missing"));
        mockMvc.perform(get("/api/v1/sprints/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPRINT_NOT_FOUND"));
    }

    @Test
    void returns404ForUnknownSprintInAnalysis() throws Exception {
        when(readService.analyzeSprint("missing")).thenThrow(new SprintNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/sprints/missing/analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPRINT_NOT_FOUND"));
    }

    @Test
    void returns400ForBlankSprintIdThroughService() throws Exception {
        ResponseEntity<ErrorResponse> response = new ApiExceptionHandler()
                .invalidSprintId(new InvalidSprintIdException());
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("INVALID_SPRINT_ID");
    }

    @Test
    void returnsSafe500ErrorWithoutStackTrace() throws Exception {
        when(readService.listSprints()).thenThrow(new RuntimeException("internal-secret"));

        mockMvc.perform(get("/api/v1/sprints"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal-secret"))));
    }

    @Test
    void healthEndpointReturnsDeterministicNotEvaluableStatus() throws Exception {
        when(readService.getSprint("S-1")).thenReturn(new SprintDetailResponse(
                "S-1", "Alpha", "CLOSED", null, null, null, 5, null,
                12, null, null, null, null, null, null));
        when(readService.loadResult()).thenReturn(loadResult());
        when(healthService.analyze(loadResult(), "S-1")).thenReturn(
                new SprintHealthResult("S-1", HealthStatus.NOT_EVALUABLE,
                        List.of(), List.of("No numerical thresholds are established."),
                        List.of()));

        mockMvc.perform(get("/api/v1/sprints/S-1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value("S-1"))
                .andExpect(jsonPath("$.overallStatus").value("NOT_EVALUABLE"));
    }

    @Test
    void trendsEndpointReturnsObservations() throws Exception {
        when(historicalService.trends()).thenReturn(
                new com.ablmvp.sprintassistant.api.dto.TrendResponse(
                        List.of(), "startDate ascending", List.of("No forecasting")));
        mockMvc.perform(get("/api/v1/sprints/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordering").exists())
                .andExpect(jsonPath("$.observations").isArray());
    }

    @Test
    void compareEndpointReturnsEmptyLimitationForNoIds() throws Exception {
        when(historicalService.compare(null)).thenReturn(
                new com.ablmvp.sprintassistant.api.dto.SprintComparisonResponse(
                        List.of(), List.of("At least one sprintId query parameter is required.")));
        mockMvc.perform(get("/api/v1/sprints/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints").isEmpty())
                .andExpect(jsonPath("$.limitations[0]").exists());
    }

    @Test
    void analysisPreservesAvailabilityNullsAndWarnings() throws Exception {
        when(readService.getSprint("S-1")).thenReturn(new SprintDetailResponse(
                "S-1", "Alpha", "CLOSED", null, null, null, 5, null,
                12, null, null, null, null, null, null));
        LoadResult loadResult = loadResult();
        when(readService.analyzeSprint("S-1")).thenReturn(
                ApiMapper.toAnalysis(metrics(loadResult)));

        mockMvc.perform(get("/api/v1/sprints/S-1/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceFacts.recordedIssueCount.value").value(12))
                .andExpect(jsonPath("$.sourceFacts.recordedCompletedCount.value").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.sourceFacts.recordedCompletedCount.availability").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.distinctIssueCount.availability").value("AVAILABLE_WITH_WARNINGS"))
                .andExpect(jsonPath("$.distinctIssueCount.findingCodes[0]").value("DUPLICATE_LINK_PAIR"));
    }

    private static LoadResult loadResult() {
        return new LoadResult(List.of(), List.of(), List.of(), List.of(), List.of(),
                new DatasetProvenance("Synthetic", DataKind.SYNTHETIC_FIXTURE,
                        "Synthetic", "synthetic", "No real data",
                        "Test", List.of(), Instant.EPOCH, List.of()),
                UsabilityState.USABLE);
    }

    private static SprintMetricsResult metrics(LoadResult loadResult) {
        MetricResult<Integer> issueCount = new MetricResult<>(12,
                MetricAvailability.AVAILABLE, List.of(), List.of("Sprint.recordedIssueCount"), List.of());
        MetricResult<Integer> completed = new MetricResult<>(null,
                MetricAvailability.INSUFFICIENT_DATA, List.of("The source field is null"),
                List.of("Sprint.recordedCompletedCount"), List.of());
        MetricResult<Integer> distinct = new MetricResult<>(1,
                MetricAvailability.AVAILABLE_WITH_WARNINGS,
                List.of("Repeated sprint/issue link rows were de-duplicated"),
                List.of("IssueSprintLink.issueKey"), List.of("DUPLICATE_LINK_PAIR"));

        SprintSourceFacts sourceFacts = new SprintSourceFacts(
                "S-1", issueCount, completed,
                missingInteger("Sprint.recordedNotCompletedCount"),
                missingInteger("Sprint.recordedPuntedCount"),
                missingInteger("Sprint.recordedCompletedElsewhereCount"),
                missingInteger("Sprint.lengthDays"),
                new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                        List.of("The source field is null"),
                        List.of("Sprint.recordedAddedIssueKeys"), List.of()),
                missingDecimal("Sprint.estimateSums.completedInitial"),
                missingDecimal("Sprint.estimateSums.completedCurrent"),
                missingDecimal("Sprint.estimateSums.puntedInitial"),
                missingDecimal("Sprint.estimateSums.puntedCurrent"),
                missingDecimal("Sprint.estimateSums.notCompletedInitial"),
                missingDecimal("Sprint.estimateSums.notCompletedCurrent"),
                missingDecimal("Sprint.estimateSums.completedElsewhereInitial"),
                missingDecimal("Sprint.estimateSums.completedElsewhereCurrent"));

        MetricResult<com.ablmvp.sprintassistant.metrics.AssigneeDistribution> assignee =
                new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                        List.of("No known issue membership"), List.of("IssueSprintLink.assignee"), List.of());

        return new SprintMetricsResult(
                "S-1", sourceFacts, distinct, assignee,
                loadResult.provenance(), UsabilityState.USABLE,
                List.of(), List.of());
    }

    private static MetricResult<Integer> missingInteger(String field) {
        return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                List.of("The source field is null"), List.of(field), List.of());
    }

    private static MetricResult<BigDecimal> missingDecimal(String field) {
        return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                List.of("The source field is null"), List.of(field), List.of());
    }
}
