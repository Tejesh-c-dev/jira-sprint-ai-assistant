package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.data.result.DatasetLoadOutcome;
import com.ablmvp.sprintassistant.domain.UsabilityState;

public record DatasetQualityResponse(
        DatasetLoadOutcome loadOutcome,
        UsabilityState usabilityState,
        int findingCount,
        int blockingFindingCount,
        int quarantinedRecordCount,
        int unknownSprintReferenceCount,
        int unknownIssueReferenceCount,
        int duplicateIssueRecordCount,
        ProvenanceResponse provenance) {
}
