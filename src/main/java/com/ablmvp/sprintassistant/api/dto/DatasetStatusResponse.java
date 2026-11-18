package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.data.result.DatasetLoadOutcome;
import com.ablmvp.sprintassistant.domain.UsabilityState;

public record DatasetStatusResponse(
        DatasetLoadOutcome loadStatus,
        boolean dataLoaded,
        UsabilityState usabilityState,
        int findingCount,
        int blockingFindingCount,
        int quarantinedRecordCount,
        ProvenanceResponse provenance) {
}
