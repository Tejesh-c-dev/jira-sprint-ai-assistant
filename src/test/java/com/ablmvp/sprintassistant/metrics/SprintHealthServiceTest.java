package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SourceRef;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SprintHealthServiceTest {
    private final SprintHealthService service = new SprintHealthService();

    @Test
    void returnsNotEvaluableForEverySprintWithoutApprovedThresholds() {
        Sprint sprint = sprint("S-1");
        LoadResult result = load(List.of(sprint), UsabilityState.USABLE);

        SprintHealthResult health = service.analyze(result, "S-1");

        assertThat(health.overallStatus()).isEqualTo(HealthStatus.NOT_EVALUABLE);
        assertThat(health.dimensions()).extracting(HealthDimensionResult::dimension)
                .containsExactly(
                        "Completion performance",
                        "Scope stability",
                        "Carryover",
                        "Estimate consistency",
                        "Workload distribution",
                        "Data completeness");
    }

    @Test
    void missingSprintReturnsInsufficientData() {
        LoadResult result = load(List.of(), UsabilityState.USABLE);
        SprintHealthResult health = service.analyze(result, "missing");
        assertThat(health.overallStatus()).isEqualTo(HealthStatus.INSUFFICIENT_DATA);
        assertThat(health.explanation()).contains("Sprint not found in the loaded sprint records");
    }

    @Test
    void warnsWhenUsabilityContainsWarnings() {
        Sprint sprint = sprint("S-1");
        LoadResult result = load(List.of(sprint), UsabilityState.USABLE_WITH_WARNINGS);
        SprintHealthResult health = service.analyze(result, "S-1");
        assertThat(health.dimensions().get(5).status())
                .isEqualTo(HealthDimensionStatus.AVAILABLE_WITH_WARNINGS);
    }

    private static LoadResult load(List<Sprint> sprints, UsabilityState usability) {
        DatasetProvenance provenance = new DatasetProvenance(
                "Synthetic", DataKind.SYNTHETIC_FIXTURE,
                "Synthetic", "synthetic", "Test",
                "Test", List.of(), Instant.EPOCH, List.of());
        return new LoadResult(List.of(), sprints, List.of(), List.of(),
                List.of(), provenance, usability);
    }

    private static Sprint sprint(String id) {
        return new Sprint(id, "Synthetic", "CLOSED", null, null, null, 5,
                2, 1, 1, 0, 0, 0, List.of(),
                new SprintEstimateSums(null, null, null, null, null, null, null, null),
                new SourceRef(FileRole.SPRINTS, 1, null));
    }
}
