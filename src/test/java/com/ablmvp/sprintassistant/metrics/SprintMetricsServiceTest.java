package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.data.result.FindingSeverity;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.data.result.QuarantinedRecord;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.IssueSprintLink;
import com.ablmvp.sprintassistant.domain.SourceRef;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SprintMetricsServiceTest {
    private final SprintMetricsService service = new SprintMetricsService();

    @Test
    void returnsRecordedSourceFactsForNormalSprint() {
        Sprint sprint = sprint("S-1", 12, 8, 2, 1, 1, 10, List.of("I-3"),
                estimates("1", "2", "3", "4", "5", "6", "7", "8"));
        LoadResult result = load(List.of(issue("I-1", "Alice")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice")), List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.sourceFacts().recordedIssueCount().value()).isEqualTo(12);
        assertThat(metrics.sourceFacts().recordedIssueCount().availability()).isEqualTo(MetricAvailability.AVAILABLE);
        assertThat(metrics.sourceFacts().recordedCompletedCount().value()).isEqualTo(8);
        assertThat(metrics.sourceFacts().recordedNotCompletedCount().value()).isEqualTo(2);
        assertThat(metrics.sourceFacts().recordedPuntedCount().value()).isEqualTo(1);
        assertThat(metrics.sourceFacts().recordedCompletedElsewhereCount().value()).isEqualTo(1);
        assertThat(metrics.sourceFacts().recordedSprintLength().value()).isEqualTo(10);
        assertThat(metrics.sourceFacts().recordedAddedIssueKeys().value()).containsExactly("I-3");
        assertThat(metrics.sourceFacts().completedIssuesInitialEstimateSum().value())
                .isEqualByComparingTo("1");
        assertThat(metrics.distinctIssueCount().value()).isEqualTo(1);
        assertThat(metrics.assigneeDistribution().value().issueCounts()).containsEntry("Alice", 1);
    }

    @Test
    void emptySprintHasInsufficientDistinctAndAssigneeMetrics() {
        Sprint sprint = sprint("S-1", 0, 0, 0, 0, 0, null, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(), List.of(sprint), List.of(), List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.distinctIssueCount().availability()).isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(metrics.assigneeDistribution().availability()).isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(metrics.sourceFacts().recordedIssueCount().value()).isZero();
        assertThat(metrics.sourceFacts().recordedSprintLength().value()).isNull();
        assertThat(metrics.sourceFacts().recordedSprintLength().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
    }

    @Test
    void preservesNullSourceCountsAndMissingEstimates() {
        Sprint sprint = sprint("S-1", null, null, null, null, null, null, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(), List.of(sprint), List.of(), List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.sourceFacts().recordedIssueCount().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(metrics.sourceFacts().recordedIssueCount().value()).isNull();
        assertThat(metrics.sourceFacts().completedIssuesInitialEstimateSum().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(metrics.sourceFacts().completedIssuesInitialEstimateSum().value()).isNull();
    }

    @Test
    void duplicateSprintIssueLinksAreDeDuplicated() {
        Sprint sprint = sprint("S-1", 2, 1, 1, 0, 0, 5, null,
                estimates("1", "1", null, null, null, null, null, null));
        LoadResult result = load(List.of(issue("I-1", "Alice")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice"), link("S-1", "I-1", "Alice")),
                List.of(finding("DUPLICATE_LINK_PAIR", FileRole.ISSUE_SPRINT_SUMMARY)),
                UsabilityState.USABLE_WITH_WARNINGS);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.distinctIssueCount().value()).isEqualTo(1);
        assertThat(metrics.distinctIssueCount().availability())
                .isEqualTo(MetricAvailability.AVAILABLE_WITH_WARNINGS);
        assertThat(metrics.distinctIssueCount().findingCodes()).contains("DUPLICATE_LINK_PAIR");
        assertThat(metrics.assigneeDistribution().value().issueCounts()).containsEntry("Alice", 1);
    }

    @Test
    void unknownIssueReferencesAreNotCounted() {
        Sprint sprint = sprint("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(), List.of(sprint),
                List.of(link("S-1", "UNKNOWN", null)),
                List.of(finding("UNKNOWN_ISSUE_REFERENCE", FileRole.ISSUE_SPRINT_SUMMARY)),
                UsabilityState.USABLE_WITH_WARNINGS);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.distinctIssueCount().value()).isNull();
        assertThat(metrics.distinctIssueCount().availability()).isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(metrics.distinctIssueCount().findingCodes()).contains("UNKNOWN_ISSUE_REFERENCE");
        assertThat(metrics.assigneeDistribution().value()).isNull();
    }

    @Test
    void detectsConflictingAssigneeObservationsAndPreservesEvidence() {
        Sprint sprint = sprint("S-1", 2, 1, 1, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(issue("I-1", "Bob")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice"), link("S-1", "I-1", "Carol")),
                List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");
        AssigneeDistribution distribution = metrics.assigneeDistribution().value();

        assertThat(metrics.assigneeDistribution().availability())
                .isEqualTo(MetricAvailability.AVAILABLE_WITH_WARNINGS);
        assertThat(distribution.issueCounts()).doesNotContainKey("UNASSIGNED");
        assertThat(distribution.issueCounts()).isEmpty();
        assertThat(distribution.conflicts()).singleElement().satisfies(conflict ->
                assertThat(conflict.issueKey()).isEqualTo("I-1"));
        assertThat(distribution.conflicts().get(0).resolvedAssignee()).isNull();
        assertThat(distribution.conflicts().get(0).observedAssignees())
                .containsExactly("Alice", "Bob", "Carol");
    }

    @Test
    void missingAssigneeUsesUnassignedRepresentation() {
        Sprint sprint = sprint("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(issue("I-1", null)), List.of(sprint),
                List.of(link("S-1", "I-1", null)), List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.assigneeDistribution().availability())
                .isEqualTo(MetricAvailability.AVAILABLE_WITH_WARNINGS);
        assertThat(metrics.assigneeDistribution().value().issueCounts()).isEmpty();
        assertThat(metrics.assigneeDistribution().value().conflicts()).singleElement()
                .extracting(AssigneeConflict::resolvedAssignee).isEqualTo(null);
    }

    @Test
    void unknownIssueFindingAffectsOnlyItsSprint() {
        Sprint sprint1 = sprintAt("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint sprint2 = sprintAt("S-2", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(
                List.of(issue("I-1", "Alice"), issue("I-2", "Bob")),
                List.of(sprint1, sprint2),
                List.of(linkAt("S-1", "UNKNOWN", null, 1),
                        linkAt("S-2", "I-2", "Bob", 2)),
                List.of(findingAt("UNKNOWN_ISSUE_REFERENCE", FileRole.ISSUE_SPRINT_SUMMARY, 1)),
                UsabilityState.USABLE_WITH_WARNINGS);

        assertThat(service.analyze(result, "S-1").distinctIssueCount().findingCodes())
                .contains("UNKNOWN_ISSUE_REFERENCE");
        assertThat(service.analyze(result, "S-1").distinctIssueCount().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(service.analyze(result, "S-2").distinctIssueCount().value()).isEqualTo(1);
        assertThat(service.analyze(result, "S-2").distinctIssueCount().findingCodes()).isEmpty();
    }

    @Test
    void duplicateLinkFindingAffectsOnlyItsSprint() {
        Sprint sprint1 = sprintAt("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint sprint2 = sprintAt("S-2", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(
                List.of(issue("I-1", "Alice"), issue("I-2", "Bob")),
                List.of(sprint1, sprint2),
                List.of(linkAt("S-1", "I-1", "Alice", 1),
                        linkAt("S-1", "I-1", "Alice", 2),
                        linkAt("S-2", "I-2", "Bob", 3)),
                List.of(findingAt("DUPLICATE_LINK_PAIR", FileRole.ISSUE_SPRINT_SUMMARY, 1)),
                UsabilityState.USABLE_WITH_WARNINGS);

        assertThat(service.analyze(result, "S-1").distinctIssueCount().value()).isEqualTo(1);
        assertThat(service.analyze(result, "S-1").distinctIssueCount().findingCodes())
                .contains("DUPLICATE_LINK_PAIR");
        assertThat(service.analyze(result, "S-2").distinctIssueCount().value()).isEqualTo(1);
        assertThat(service.analyze(result, "S-2").distinctIssueCount().findingCodes()).isEmpty();
    }

    @Test
    void sourceRowFindingAffectsOnlyCorrespondingSprintSourceFacts() {
        Sprint sprint1 = sprintAt("S-1", 1, null, null, null, null, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint sprint2 = sprintAt("S-2", 2, null, null, null, null, 5, null,
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(List.of(), List.of(sprint1, sprint2), List.of(),
                List.of(findingAt("DATE_INVALID", FileRole.SPRINTS, 1)),
                UsabilityState.USABLE_WITH_WARNINGS);

        assertThat(service.analyze(result, "S-1").sourceFacts().recordedIssueCount().findingCodes())
                .contains("DATE_INVALID");
        assertThat(service.analyze(result, "S-1").sourceFacts().recordedIssueCount().availability())
                .isEqualTo(MetricAvailability.AVAILABLE_WITH_WARNINGS);
        assertThat(service.analyze(result, "S-2").sourceFacts().recordedIssueCount().findingCodes()).isEmpty();
        assertThat(service.analyze(result, "S-2").sourceFacts().recordedIssueCount().availability())
                .isEqualTo(MetricAvailability.AVAILABLE);
    }

    @Test
    void datasetLevelFindingAffectsBothSprints() {
        Sprint sprint1 = sprintAt("S-1", 1, null, null, null, null, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint sprint2 = sprintAt("S-2", 2, null, null, null, null, 5, null,
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(List.of(), List.of(sprint1, sprint2), List.of(),
                List.of(findingAt("HEADER_MISSING", FileRole.SPRINTS, null)),
                UsabilityState.USABLE_WITH_WARNINGS);

        assertThat(service.analyze(result, "S-1").sourceFacts().recordedIssueCount().findingCodes())
                .contains("HEADER_MISSING");
        assertThat(service.analyze(result, "S-2").sourceFacts().recordedIssueCount().findingCodes())
                .contains("HEADER_MISSING");
    }

    @Test
    void linkAssigneeOverridesIssueAssigneeAndPreservesConflict() {
        Sprint sprint = sprint("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null));
        LoadResult result = load(List.of(issue("I-1", "Bob")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice")), List.of(), UsabilityState.USABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.assigneeDistribution().value().issueCounts())
                .containsEntry("Alice", 1);
        assertThat(metrics.assigneeDistribution().value().issueCounts())
                .doesNotContainKey("Bob");
        assertThat(metrics.assigneeDistribution().value().conflicts()).singleElement()
                .satisfies(conflict -> {
                    assertThat(conflict.resolvedAssignee()).isEqualTo("Alice");
                    assertThat(conflict.observedAssignees()).containsExactly("Alice", "Bob");
                });
    }

    @Test
    void emptyAndIncompleteMembershipRemainDistinct() {
        Sprint emptySprint = sprintAt("S-EMPTY", 0, 0, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint incompleteSprint = sprintAt("S-INCOMPLETE", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(List.of(), List.of(emptySprint, incompleteSprint),
                List.of(linkAt("S-INCOMPLETE", "UNKNOWN", null, 1)),
                List.of(findingAt("UNKNOWN_ISSUE_REFERENCE", FileRole.ISSUE_SPRINT_SUMMARY, 1)),
                UsabilityState.USABLE_WITH_WARNINGS);

        SprintMetricsResult empty = service.analyze(result, "S-EMPTY");
        SprintMetricsResult incomplete = service.analyze(result, "S-INCOMPLETE");

        assertThat(empty.distinctIssueCount().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(empty.distinctIssueCount().reasons())
                .contains("No IssueSprintLink membership rows were found for the sprint");
        assertThat(incomplete.distinctIssueCount().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(incomplete.distinctIssueCount().reasons())
                .contains("Membership coverage is incomplete because unknown Issue references were excluded");
    }

    @Test
    void nullAndEmptyAddedIssueKeyListsRemainDistinct() {
        Sprint nullSprint = sprintAt("S-NULL", 1, null, null, null, null, 5, null,
                estimates(null, null, null, null, null, null, null, null), 1);
        Sprint emptySprint = sprintAt("S-EMPTY", 1, null, null, null, null, 5, List.of(),
                estimates(null, null, null, null, null, null, null, null), 2);
        LoadResult result = load(List.of(), List.of(nullSprint, emptySprint), List.of(),
                List.of(), UsabilityState.USABLE);

        assertThat(service.analyze(result, "S-NULL").sourceFacts().recordedAddedIssueKeys().value()).isNull();
        assertThat(service.analyze(result, "S-NULL").sourceFacts().recordedAddedIssueKeys().availability())
                .isEqualTo(MetricAvailability.INSUFFICIENT_DATA);
        assertThat(service.analyze(result, "S-EMPTY").sourceFacts().recordedAddedIssueKeys().value()).isEmpty();
        assertThat(service.analyze(result, "S-EMPTY").sourceFacts().recordedAddedIssueKeys().availability())
                .isEqualTo(MetricAvailability.AVAILABLE);
    }

    @Test
    void partialDatasetWarningKeepsMetricAvailableWithWarnings() {
        Sprint sprint = sprint("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates(null, null, null, null, null, null, null, null));
        ValidationFinding finding = finding("DATE_INVALID", FileRole.SPRINTS);
        LoadResult result = load(List.of(issue("I-1", "Alice")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice")), List.of(finding),
                UsabilityState.USABLE_WITH_WARNINGS);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.sourceFacts().recordedIssueCount().availability())
                .isEqualTo(MetricAvailability.AVAILABLE_WITH_WARNINGS);
        assertThat(metrics.sourceFacts().recordedIssueCount().findingCodes()).contains("DATE_INVALID");
        assertThat(metrics.distinctIssueCount().availability()).isEqualTo(MetricAvailability.AVAILABLE);
    }

    @Test
    void unusableDatasetMakesMetricsUnavailable() {
        Sprint sprint = sprint("S-1", 1, 1, 0, 0, 0, 5, null,
                estimates("1", null, null, null, null, null, null, null));
        ValidationFinding finding = finding("HEADER_MISSING", FileRole.SPRINTS);
        LoadResult result = load(List.of(issue("I-1", "Alice")), List.of(sprint),
                List.of(link("S-1", "I-1", "Alice")), List.of(finding),
                UsabilityState.UNUSABLE);

        SprintMetricsResult metrics = service.analyze(result, "S-1");

        assertThat(metrics.sourceFacts().recordedIssueCount().availability())
                .isEqualTo(MetricAvailability.UNAVAILABLE);
        assertThat(metrics.distinctIssueCount().availability()).isEqualTo(MetricAvailability.UNAVAILABLE);
        assertThat(metrics.assigneeDistribution().availability()).isEqualTo(MetricAvailability.UNAVAILABLE);
    }

    private static LoadResult load(List<Issue> issues, List<Sprint> sprints,
                                   List<IssueSprintLink> links, List<ValidationFinding> findings,
                                   UsabilityState usability) {
        DatasetProvenance provenance = new DatasetProvenance(
                "Synthetic test dataset", DataKind.SYNTHETIC_FIXTURE,
                "Synthetic fixture", "synthetic", "Not a real dataset",
                "Test data", List.of(), java.time.Instant.EPOCH, List.of());
        return new LoadResult(issues, sprints, links, List.of(), findings, provenance, usability);
    }

    private static Sprint sprint(String id, Integer issueCount, Integer completed,
                                 Integer notCompleted, Integer punted, Integer elsewhere,
                                 Integer length, List<String> added,
                                 SprintEstimateSums estimates) {
        return new Sprint(id, "Synthetic Sprint", "Closed", null, null, null, length,
                null, issueCount, completed, notCompleted, punted, elsewhere, added,
                estimates, sourceRef(FileRole.SPRINTS, 1));
    }

    private static Sprint sprintAt(String id, Integer issueCount, Integer completed,
                                   Integer notCompleted, Integer punted, Integer elsewhere,
                                   Integer length, List<String> added,
                                   SprintEstimateSums estimates, int sourceRecordNumber) {
        return new Sprint(id, "Synthetic Sprint", "Closed", null, null, null, length,
                null, issueCount, completed, notCompleted, punted, elsewhere, added,
                estimates, sourceRef(FileRole.SPRINTS, sourceRecordNumber));
    }

    private static SprintEstimateSums estimates(String completedInitial, String completedCurrent,
                                                String puntedInitial, String puntedCurrent,
                                                String notCompletedInitial, String notCompletedCurrent,
                                                String elsewhereInitial, String elsewhereCurrent) {
        return new SprintEstimateSums(decimal(completedInitial), decimal(completedCurrent),
                decimal(puntedInitial), decimal(puntedCurrent),
                decimal(notCompletedInitial), decimal(notCompletedCurrent),
                decimal(elsewhereInitial), decimal(elsewhereCurrent));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static Issue issue(String key, String assignee) {
        return new Issue(key, null, null, null, null, null, assignee, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                sourceRef(FileRole.ISSUES, 1));
    }

    private static IssueSprintLink link(String sprintId, String issueKey, String assignee) {
        return new IssueSprintLink("ISSUE_SPRINT_SUMMARY-1", sprintId, issueKey, null, null,
                assignee, null, null, null, null, null,
                sourceRef(FileRole.ISSUE_SPRINT_SUMMARY, 1));
    }

    private static IssueSprintLink linkAt(String sprintId, String issueKey, String assignee,
                                          int sourceRecordNumber) {
        return new IssueSprintLink("ISSUE_SPRINT_SUMMARY-" + sourceRecordNumber,
                sprintId, issueKey, null, null, assignee, null, null, null, null, null,
                sourceRef(FileRole.ISSUE_SPRINT_SUMMARY, sourceRecordNumber));
    }

    private static SourceRef sourceRef(FileRole role, int recordNumber) {
        return new SourceRef(role, recordNumber, null);
    }

    private static ValidationFinding finding(String code, FileRole role) {
        return new ValidationFinding(role, 1, null, FindingSeverity.WARNING,
                code, "Synthetic test warning", null, false);
    }

    private static ValidationFinding findingAt(String code, FileRole role, Integer recordNumber) {
        return new ValidationFinding(role, recordNumber, null, FindingSeverity.WARNING,
                code, "Synthetic test warning", null, false);
    }
}
