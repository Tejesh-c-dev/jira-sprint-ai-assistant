package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.SourceRef;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalSprintServiceTest {
    @Test
    void sortsByStartDateWithMissingDatesLast() {
        Sprint later = sprint("S-2", LocalDate.of(2020, 2, 1));
        Sprint earlier = sprint("S-1", LocalDate.of(2020, 1, 1));
        Sprint missing = sprint("S-3", null);
        HistoricalSprintService service = service(earlier, later, missing);

        var result = service.trends();

        assertThat(result.observations()).extracting(observation -> observation.sprintId())
                .containsExactly("S-1", "S-2", "S-3");
        assertThat(result.observations()).extracting(observation -> observation.startDate())
                .containsExactly(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), null);
    }

    @Test
    void compareRequiresIdsAndOmitsUnknownIds() {
        HistoricalSprintService service = service(sprint("S-1", LocalDate.of(2020, 1, 1)),
                sprint("S-2", LocalDate.of(2020, 2, 1)));

        assertThat(service.compare(List.of()).limitations()).isNotEmpty();
        assertThat(service.compare(List.of("S-1", "missing", "S-2")).sprints())
                .extracting(item -> item.sprintId())
                .containsExactly("S-1", "S-2");
    }

    private static HistoricalSprintService service(Sprint... sprints) {
        LoadResult result = new LoadResult(
                List.of(), List.of(sprints), List.of(), List.of(), List.of(),
                new DatasetProvenance("Synthetic", DataKind.SYNTHETIC_FIXTURE,
                        "Synthetic", "synthetic", "Test",
                        "Test", List.of(), Instant.EPOCH, List.of()),
                UsabilityState.USABLE);
        return new HistoricalSprintService(
                new SprintRepository(result), new SprintMetricsService());
    }

    private static Sprint sprint(String id, LocalDate startDate) {
        return new Sprint(id, id, "CLOSED", startDate, null, null, 5,
                2, 10, 8, 1, 1, 0, List.of(),
                new SprintEstimateSums(null, null, null, null, null, null, null, null),
                new SourceRef(FileRole.SPRINTS, 1, null));
    }
}
