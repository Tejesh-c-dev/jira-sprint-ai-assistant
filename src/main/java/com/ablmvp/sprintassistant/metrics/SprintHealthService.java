package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.domain.Sprint;

import java.util.ArrayList;
import java.util.List;

public final class SprintHealthService {
    public SprintHealthResult analyze(LoadResult result, String sprintId) {
        Sprint sprint = result.sprints().stream()
                .filter(candidate -> sprintId.equals(candidate.sprintId()))
                .findFirst()
                .orElse(null);
        if (sprint == null) {
            return new SprintHealthResult(sprintId, HealthStatus.INSUFFICIENT_DATA,
                    List.of(), List.of("Sprint not found in the loaded sprint records"), List.of());
        }

        List<HealthDimensionResult> dimensions = new ArrayList<>();
        dimensions.add(new HealthDimensionResult(
                "Completion performance",
                HealthDimensionStatus.REQUIRES_HUMAN_DECISION,
                "The source completion categories and denominator have not been approved.",
                List.of("recordedCompletedCount", "recordedIssueCount"),
                List.of(),
                "Completion classification is not approved."));
        dimensions.add(new HealthDimensionResult(
                "Scope stability",
                HealthDimensionStatus.PROVISIONAL,
                "The sprint reports added issue keys, but scope encoding and denominator are unresolved.",
                List.of("recordedAddedIssueKeys"),
                resultUsabilityWarnings(result),
                "No scope-change formula is approved."));
        dimensions.add(new HealthDimensionResult(
                "Carryover",
                HealthDimensionStatus.NOT_EVALUABLE,
                "Membership across sprints is not evidence of carryover.",
                List.of("IssueSprintLink.sprintId", "IssueSprintLink.issueKey"),
                List.of(),
                "Historical carryover chronology is not established."));
        dimensions.add(new HealthDimensionResult(
                "Estimate consistency",
                HealthDimensionStatus.PROVISIONAL,
                "Individual estimate observations exist, but units and initial/current semantics are unresolved.",
                List.of("SprintEstimateSums"),
                negativeEstimateWarnings(result, sprint),
                "No estimate aggregate or consistency formula is approved."));
        dimensions.add(new HealthDimensionResult(
                "Workload distribution",
                HealthDimensionStatus.AVAILABLE_WITH_WARNINGS,
                "Known issue-assignee observations can be counted separately from unresolved conflict evidence.",
                List.of("IssueSprintLink.assignee", "Issue.assignee"),
                resultUsabilityWarnings(result),
                null));
        dimensions.add(new HealthDimensionResult(
                "Data completeness",
                result.usabilityState().name().equals("USABLE")
                        ? HealthDimensionStatus.AVAILABLE
                        : HealthDimensionStatus.AVAILABLE_WITH_WARNINGS,
                "Load usability and validation evidence are available.",
                List.of("LoadResult.usabilityState", "LoadResult.findings", "LoadResult.quarantinedRecords"),
                resultUsabilityWarnings(result),
                null));

        List<String> warnings = result.findings().stream()
                .map(ValidationFinding::reason)
                .distinct()
                .limit(20)
                .toList();
        return new SprintHealthResult(
                sprintId,
                HealthStatus.NOT_EVALUABLE,
                dimensions,
                List.of("No numerical health thresholds or approved health formula are established."),
                warnings);
    }

    private static List<String> resultUsabilityWarnings(LoadResult result) {
        if (result.usabilityState().name().equals("USABLE")) return List.of();
        return result.findings().stream()
                .map(ValidationFinding::reason)
                .distinct()
                .limit(20)
                .toList();
    }

    private static List<String> negativeEstimateWarnings(LoadResult result, Sprint sprint) {
        if (sprint.estimateSums() == null) return List.of();
        List<String> warnings = new ArrayList<>();
        java.util.function.Consumer<String> addNegative = label -> warnings.add(label);
        java.math.BigDecimal[] values = {
                sprint.estimateSums().completedInitial(),
                sprint.estimateSums().completedCurrent(),
                sprint.estimateSums().puntedInitial(),
                sprint.estimateSums().puntedCurrent(),
                sprint.estimateSums().notCompletedInitial(),
                sprint.estimateSums().notCompletedCurrent(),
                sprint.estimateSums().completedElsewhereInitial(),
                sprint.estimateSums().completedElsewhereCurrent()
        };
        for (java.math.BigDecimal value : values) {
            if (value != null && value.signum() < 0) {
                addNegative.accept("Negative estimate source value is present and unresolved.");
                break;
            }
        }
        return warnings;
    }
}
