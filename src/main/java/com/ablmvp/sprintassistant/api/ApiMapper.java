package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.*;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.data.result.QuarantinedRecord;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.FileFingerprint;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.metrics.AssigneeConflict;
import com.ablmvp.sprintassistant.metrics.AssigneeDistribution;
import com.ablmvp.sprintassistant.metrics.MetricResult;
import com.ablmvp.sprintassistant.metrics.SprintMetricsResult;

import java.util.List;

public final class ApiMapper {
    private ApiMapper() {
    }

    public static SprintSummaryResponse toSummary(Sprint sprint) {
        return new SprintSummaryResponse(
                sprint.sprintId(), sprint.name(), sprint.state(),
                sprint.startDate(), sprint.endDate(), sprint.completeDate(), sprint.lengthDays());
    }

    public static SprintDetailResponse toDetail(Sprint sprint) {
        return new SprintDetailResponse(
                sprint.sprintId(), sprint.name(), sprint.state(),
                sprint.startDate(), sprint.endDate(), sprint.completeDate(),
                sprint.lengthDays(), sprint.developerCount(),
                sprint.recordedIssueCount(), sprint.recordedCompletedCount(),
                sprint.recordedNotCompletedCount(), sprint.recordedPuntedCount(),
                sprint.recordedCompletedElsewhereCount(),
                sprint.recordedAddedIssueKeys(),
                toEstimateSums(sprint.estimateSums()));
    }

    private static SprintEstimateSumsResponse toEstimateSums(SprintEstimateSums sums) {
        if (sums == null) return null;
        return new SprintEstimateSumsResponse(
                sums.completedInitial(), sums.completedCurrent(),
                sums.puntedInitial(), sums.puntedCurrent(),
                sums.notCompletedInitial(), sums.notCompletedCurrent(),
                sums.completedElsewhereInitial(), sums.completedElsewhereCurrent());
    }

    public static SprintAnalysisResponse toAnalysis(SprintMetricsResult metrics) {
        var source = metrics.sourceFacts();
        var assignee = metrics.assigneeDistribution().value();
        MetricResultResponse<AssigneeDistributionResponse> assigneeResponse =
                assignee == null
                        ? new MetricResultResponse<>(
                                null, metrics.assigneeDistribution().availability(),
                                metrics.assigneeDistribution().reasons(),
                                metrics.assigneeDistribution().sourceFields(),
                                metrics.assigneeDistribution().findingCodes())
                        : new MetricResultResponse<>(
                                toAssigneeDistribution(assignee),
                                metrics.assigneeDistribution().availability(),
                                metrics.assigneeDistribution().reasons(),
                                metrics.assigneeDistribution().sourceFields(),
                                metrics.assigneeDistribution().findingCodes());

        return new SprintAnalysisResponse(
                metrics.sprintId(),
                new SprintSourceFactsResponse(
                        toMetric(source.recordedIssueCount()),
                        toMetric(source.recordedCompletedCount()),
                        toMetric(source.recordedNotCompletedCount()),
                        toMetric(source.recordedPuntedCount()),
                        toMetric(source.recordedCompletedElsewhereCount()),
                        toMetric(source.recordedSprintLength()),
                        toMetric(source.recordedAddedIssueKeys()),
                        toMetric(source.completedIssuesInitialEstimateSum()),
                        toMetric(source.completedIssuesCurrentEstimateSum()),
                        toMetric(source.puntedIssuesInitialEstimateSum()),
                        toMetric(source.puntedIssuesCurrentEstimateSum()),
                        toMetric(source.notCompletedIssuesInitialEstimateSum()),
                        toMetric(source.notCompletedIssuesCurrentEstimateSum()),
                        toMetric(source.completedElsewhereInitialEstimateSum()),
                        toMetric(source.completedElsewhereCurrentEstimateSum())),
                toMetric(metrics.distinctIssueCount()),
                toMetric(metrics.unknownIssueReferenceCount()),
                toMetric(metrics.quarantinedIssueRecordCount()),
                assigneeResponse,
                toProvenance(metrics.provenance()),
                metrics.usabilityState(),
                metrics.findings().stream().map(ApiMapper::toFinding).toList(),
                metrics.quarantinedRecords().stream().map(ApiMapper::toQuarantine).toList());
    }

    public static <T> MetricResultResponse<T> toMetric(MetricResult<T> result) {
        return new MetricResultResponse<>(
                result.value(), result.availability(), result.reasons(),
                result.sourceFields(), result.findingCodes());
    }

    public static SprintHealthResponse toHealth(com.ablmvp.sprintassistant.metrics.SprintHealthResult health) {
        List<HealthDimensionResponse> dimensions = health.dimensions().stream()
                .map(dimension -> new HealthDimensionResponse(
                        dimension.dimension(), dimension.status(), dimension.explanation(),
                        dimension.evidence(), dimension.warnings(), dimension.reason()))
                .toList();
        return new SprintHealthResponse(
                health.sprintId(), health.overallStatus(), dimensions,
                health.explanation(), health.warnings());
    }

    private static AssigneeDistributionResponse toAssigneeDistribution(AssigneeDistribution distribution) {
        List<AssigneeConflictResponse> conflicts = distribution.conflicts().stream()
                .map(ApiMapper::toConflict)
                .toList();
        return new AssigneeDistributionResponse(distribution.issueCounts(), conflicts);
    }

    private static AssigneeConflictResponse toConflict(AssigneeConflict conflict) {
        return new AssigneeConflictResponse(
                conflict.issueKey(), conflict.resolvedAssignee(), conflict.observedAssignees());
    }

    public static ProvenanceResponse toProvenance(DatasetProvenance provenance) {
        if (provenance == null) return null;
        List<FileFingerprintResponse> fingerprints = provenance.fileFingerprints().stream()
                .map(ApiMapper::toFingerprint)
                .toList();
        return new ProvenanceResponse(
                provenance.datasetName(), provenance.dataKind(), provenance.attribution(),
                provenance.sourceRepository(), provenance.licenceStatus(), provenance.snapshotLabel(),
                provenance.knownLimitations(), provenance.loadedAt(), fingerprints);
    }

    private static FileFingerprintResponse toFingerprint(FileFingerprint fingerprint) {
        return new FileFingerprintResponse(
                fingerprint.fileRole(), fingerprint.fileName(),
                fingerprint.byteSize(), fingerprint.sha256());
    }

    private static ValidationFindingResponse toFinding(ValidationFinding finding) {
        return new ValidationFindingResponse(
                finding.fileRole(), finding.recordNumber(), finding.field(),
                finding.severity(), finding.errorCode(), finding.reason(),
                finding.rawValue(), finding.blocking());
    }

    private static QuarantinedRecordResponse toQuarantine(QuarantinedRecord record) {
        var source = record.sourceRef();
        return new QuarantinedRecordResponse(
                new SourceRefResponse(source.fileRole(), source.recordNumber(), source.rawLineNumber()),
                record.reason(), record.linkId());
    }
}
