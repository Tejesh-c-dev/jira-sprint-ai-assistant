package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.data.result.DatasetLoadOutcome;
import com.ablmvp.sprintassistant.data.result.FindingSeverity;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasetStatusController.class)
class DatasetStatusControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoadResult loadResult;

    @Test
    void reportsDatasetQualityCountsWithoutInventingValues() throws Exception {
        when(loadResult.loadOutcome()).thenReturn(DatasetLoadOutcome.LOADED);
        when(loadResult.usabilityState()).thenReturn(UsabilityState.USABLE_WITH_WARNINGS);
        when(loadResult.findings()).thenReturn(List.of());
        when(loadResult.quarantinedRecords()).thenReturn(List.of());
        when(loadResult.issues()).thenReturn(List.of());
        when(loadResult.sprints()).thenReturn(List.of());
        when(loadResult.issueSprintLinks()).thenReturn(List.of());
        when(loadResult.provenance()).thenReturn(provenance());

        mockMvc.perform(get("/api/v1/dataset/quality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadOutcome").value("LOADED"))
                .andExpect(jsonPath("$.usabilityState").value("USABLE_WITH_WARNINGS"))
                .andExpect(jsonPath("$.unknownSprintReferenceCount").value(0))
                .andExpect(jsonPath("$.unknownIssueReferenceCount").value(0));
    }

    @Test
    void reportsUsableLoadedDataset() throws Exception {
        when(loadResult.loadOutcome()).thenReturn(DatasetLoadOutcome.LOADED);
        when(loadResult.usabilityState()).thenReturn(UsabilityState.USABLE);
        when(loadResult.findings()).thenReturn(List.of());
        when(loadResult.quarantinedRecords()).thenReturn(List.of());
        when(loadResult.provenance()).thenReturn(provenance());

        mockMvc.perform(get("/api/v1/dataset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadStatus").value("LOADED"))
                .andExpect(jsonPath("$.dataLoaded").value(true))
                .andExpect(jsonPath("$.usabilityState").value("USABLE"));
    }

    @Test
    void reportsLoadedUsableWithWarningsDataset() throws Exception {
        when(loadResult.loadOutcome()).thenReturn(DatasetLoadOutcome.LOADED);
        when(loadResult.usabilityState()).thenReturn(UsabilityState.USABLE_WITH_WARNINGS);
        when(loadResult.findings()).thenReturn(List.of());
        when(loadResult.quarantinedRecords()).thenReturn(List.of());
        when(loadResult.provenance()).thenReturn(provenance());

        mockMvc.perform(get("/api/v1/dataset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadStatus").value("LOADED"))
                .andExpect(jsonPath("$.dataLoaded").value(true))
                .andExpect(jsonPath("$.usabilityState").value("USABLE_WITH_WARNINGS"));
    }

    @Test
    void reportsPartiallyLoadedUnusableDataset() throws Exception {
        when(loadResult.loadOutcome()).thenReturn(DatasetLoadOutcome.PARTIALLY_LOADED);
        when(loadResult.usabilityState()).thenReturn(UsabilityState.UNUSABLE);
        when(loadResult.findings()).thenReturn(List.of());
        when(loadResult.quarantinedRecords()).thenReturn(List.of());
        when(loadResult.provenance()).thenReturn(provenance());

        mockMvc.perform(get("/api/v1/dataset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadStatus").value("PARTIALLY_LOADED"))
                .andExpect(jsonPath("$.dataLoaded").value(true))
                .andExpect(jsonPath("$.usabilityState").value("UNUSABLE"));
    }

    @Test
    void reportsActualUsabilityAndFindingsWithoutFabricatingSuccess() throws Exception {
        when(loadResult.loadOutcome()).thenReturn(DatasetLoadOutcome.LOAD_FAILED);
        when(loadResult.usabilityState()).thenReturn(UsabilityState.UNUSABLE);
        when(loadResult.findings()).thenReturn(List.of(
                new ValidationFinding(FileRole.SPRINTS, null, null, FindingSeverity.ERROR,
                        "FILE_MISSING", "required CSV file does not exist", "data/sprints.csv", true)));
        when(loadResult.quarantinedRecords()).thenReturn(List.of());
        when(loadResult.provenance()).thenReturn(new DatasetProvenance(
                "Synthetic", DataKind.SYNTHETIC_FIXTURE,
                "Synthetic", "synthetic", "DATA-02 blocked",
                "Test", List.of(), Instant.EPOCH, List.of()));

        mockMvc.perform(get("/api/v1/dataset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataLoaded").value(false))
                .andExpect(jsonPath("$.loadStatus").value("LOAD_FAILED"))
                .andExpect(jsonPath("$.usabilityState").value("UNUSABLE"))
                .andExpect(jsonPath("$.findingCount").value(1))
                .andExpect(jsonPath("$.blockingFindingCount").value(1));
    }

    private static DatasetProvenance provenance() {
        return new DatasetProvenance(
                "Synthetic", DataKind.SYNTHETIC_FIXTURE,
                "Synthetic", "synthetic", "DATA-02 blocked",
                "Test", List.of(), Instant.EPOCH, List.of());
    }
}
