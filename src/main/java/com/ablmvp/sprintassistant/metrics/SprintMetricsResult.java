package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.data.result.QuarantinedRecord;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.UsabilityState;

import java.util.List;

public record SprintMetricsResult(
        String sprintId,
        SprintSourceFacts sourceFacts,
        MetricResult<Integer> distinctIssueCount,
        MetricResult<AssigneeDistribution> assigneeDistribution,
        DatasetProvenance provenance,
        UsabilityState usabilityState,
        List<ValidationFinding> findings,
        List<QuarantinedRecord> quarantinedRecords,
        MetricResult<Integer> unknownIssueReferenceCount,
        MetricResult<Integer> quarantinedIssueRecordCount) {

    public SprintMetricsResult(
            String sprintId,
            SprintSourceFacts sourceFacts,
            MetricResult<Integer> distinctIssueCount,
            MetricResult<AssigneeDistribution> assigneeDistribution,
            DatasetProvenance provenance,
            UsabilityState usabilityState,
            List<ValidationFinding> findings,
            List<QuarantinedRecord> quarantinedRecords) {
        this(sprintId, sourceFacts, distinctIssueCount, assigneeDistribution,
                provenance, usabilityState, findings, quarantinedRecords,
                new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                        List.of("Not provided"), List.of(), List.of()),
                new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                        List.of("Not provided"), List.of(), List.of()));
    }

    public SprintMetricsResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
        quarantinedRecords = quarantinedRecords == null ? List.of() : List.copyOf(quarantinedRecords);
    }
}
