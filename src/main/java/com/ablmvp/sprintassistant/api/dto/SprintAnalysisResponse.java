package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.domain.UsabilityState;
import java.util.List;

public record SprintAnalysisResponse(
        String sprintId,
        SprintSourceFactsResponse sourceFacts,
        MetricResultResponse<Integer> distinctIssueCount,
        MetricResultResponse<Integer> unknownIssueReferenceCount,
        MetricResultResponse<Integer> quarantinedIssueRecordCount,
        MetricResultResponse<AssigneeDistributionResponse> assigneeDistribution,
        ProvenanceResponse provenance,
        UsabilityState usabilityState,
        List<ValidationFindingResponse> findings,
        List<QuarantinedRecordResponse> quarantinedRecords) {
}
