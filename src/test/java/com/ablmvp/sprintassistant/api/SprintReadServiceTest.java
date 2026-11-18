package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.SourceRef;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SprintReadServiceTest {
    @Test
    void ordersSprintsDeterministicallyAndReturnsEmptyCollection() {
        SprintReadService populated = service(
                sprint("S-2"), sprint("S-1"), sprint("S-10"));
        assertThat(populated.listSprints()).extracting(value -> value.sprintId())
                .containsExactly("S-1", "S-10", "S-2");

        SprintReadService empty = service();
        assertThat(empty.listSprints()).isEmpty();
    }

    @Test
    void returnsSprintDetailAndThrows404ExceptionForMissingSprint() {
        SprintReadService service = service(sprint("S-1"));

        assertThat(service.getSprint("S-1").sprintId()).isEqualTo("S-1");
        assertThatThrownBy(() -> service.getSprint("missing"))
                .isInstanceOf(SprintNotFoundException.class)
                .hasMessage("Sprint not found: missing");
    }

    @Test
    void rejectsBlankSprintIdWithBadRequestException() {
        SprintReadService service = service(sprint("S-1"));
        assertThatThrownBy(() -> service.getSprint(" "))
                .isInstanceOf(InvalidSprintIdException.class);
    }

    private static SprintReadService service(Sprint... sprints) {
        LoadResult loadResult = new LoadResult(
                List.of(), List.of(sprints), List.of(), List.of(), List.of(),
                new DatasetProvenance("Synthetic", DataKind.SYNTHETIC_FIXTURE,
                        "Synthetic", "synthetic", "No real data",
                        "Test", List.of(), Instant.EPOCH, List.of()),
                UsabilityState.USABLE);
        return new SprintReadService(new SprintRepository(loadResult), new SprintMetricsService());
    }

    private static Sprint sprint(String id) {
        return new Sprint(id, id, "CLOSED", null, null, null, 5, null,
                1, 1, 0, 0, 0, null,
                new SprintEstimateSums(null, null, null, null, null, null, null, null),
                new SourceRef(FileRole.SPRINTS, 1, null));
    }
}
