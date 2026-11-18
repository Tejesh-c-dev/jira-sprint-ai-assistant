package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.data.result.ValidationFinding;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.IssueSprintLink;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.SprintEstimateSums;
import com.ablmvp.sprintassistant.domain.UsabilityState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class SprintMetricsService {
    public SprintMetricsResult analyze(LoadResult result, String sprintId) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(sprintId, "sprintId must not be null");

        Sprint sprint = result.sprints().stream()
                .filter(candidate -> sprintId.equals(candidate.sprintId()))
                .findFirst()
                .orElse(null);

        SprintSourceFacts sourceFacts = sprint == null
                ? missingSourceFacts(sprintId, result)
                : sourceFacts(result, sprint);

        return new SprintMetricsResult(
                sprintId,
                sourceFacts,
                distinctIssueCount(result, sprintId),
                assigneeDistribution(result, sprintId),
                result.provenance(),
                result.usabilityState(),
                result.findings(),
                result.quarantinedRecords(),
                unknownIssueReferenceCount(result, sprintId),
                quarantinedIssueRecordCount(result));
    }

    private SprintSourceFacts sourceFacts(LoadResult result, Sprint sprint) {
        SprintEstimateSums estimates = sprint.estimateSums();
        return new SprintSourceFacts(
                sprint.sprintId(),
                integerFact(result, sprint.sprintId(), sprint.recordedIssueCount(), "Sprint.recordedIssueCount"),
                integerFact(result, sprint.sprintId(), sprint.recordedCompletedCount(), "Sprint.recordedCompletedCount"),
                integerFact(result, sprint.sprintId(), sprint.recordedNotCompletedCount(), "Sprint.recordedNotCompletedCount"),
                integerFact(result, sprint.sprintId(), sprint.recordedPuntedCount(), "Sprint.recordedPuntedCount"),
                integerFact(result, sprint.sprintId(), sprint.recordedCompletedElsewhereCount(), "Sprint.recordedCompletedElsewhereCount"),
                integerFact(result, sprint.sprintId(), sprint.lengthDays(), "Sprint.lengthDays"),
                listFact(result, sprint.sprintId(), sprint.recordedAddedIssueKeys(), "Sprint.recordedAddedIssueKeys"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.completedInitial(), "Sprint.estimateSums.completedInitial"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.completedCurrent(), "Sprint.estimateSums.completedCurrent"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.puntedInitial(), "Sprint.estimateSums.puntedInitial"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.puntedCurrent(), "Sprint.estimateSums.puntedCurrent"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.notCompletedInitial(), "Sprint.estimateSums.notCompletedInitial"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.notCompletedCurrent(), "Sprint.estimateSums.notCompletedCurrent"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.completedElsewhereInitial(), "Sprint.estimateSums.completedElsewhereInitial"),
                decimalFact(result, sprint.sprintId(), estimates == null ? null : estimates.completedElsewhereCurrent(), "Sprint.estimateSums.completedElsewhereCurrent"));
    }

    private SprintSourceFacts missingSourceFacts(String sprintId, LoadResult result) {
        MetricAvailability availability = result.usabilityState() == UsabilityState.UNUSABLE
                ? MetricAvailability.UNAVAILABLE
                : MetricAvailability.INSUFFICIENT_DATA;
        String reason = availability == MetricAvailability.UNAVAILABLE
                ? "The loaded dataset is unusable"
                : "Sprint not found in the loaded sprint records";
        MetricResult<Integer> missingInteger = new MetricResult<>(null, availability,
                List.of(reason),
                List.of(), sprintFindingCodes(result, sprintId, null));
        MetricResult<List<String>> missingList = new MetricResult<>(null, availability,
                List.of(reason),
                List.of("Sprint.recordedAddedIssueKeys"), sprintFindingCodes(result, sprintId, "Sprint.recordedAddedIssueKeys"));
        MetricResult<BigDecimal> missingDecimal = new MetricResult<>(null, availability,
                List.of(reason),
                List.of(), sprintFindingCodes(result, sprintId, null));
        return new SprintSourceFacts(sprintId, missingInteger, missingInteger, missingInteger, missingInteger,
                missingInteger, missingInteger, missingList, missingDecimal, missingDecimal, missingDecimal,
                missingDecimal, missingDecimal, missingDecimal, missingDecimal, missingDecimal);
    }

    private MetricResult<Integer> integerFact(LoadResult result, String sprintId, Integer value, String field) {
        return sourceFact(result, sprintId, value, field);
    }

    private MetricResult<BigDecimal> decimalFact(LoadResult result, String sprintId, BigDecimal value, String field) {
        return sourceFact(result, sprintId, value, field);
    }

    private MetricResult<List<String>> listFact(LoadResult result, String sprintId, List<String> value, String field) {
        return sourceFact(result, sprintId, value == null ? null : List.copyOf(value), field);
    }

    private <T> MetricResult<T> sourceFact(LoadResult result, String sprintId, T value, String field) {
        if (result.usabilityState() == UsabilityState.UNUSABLE) {
            return new MetricResult<>(null, MetricAvailability.UNAVAILABLE,
                    List.of("The loaded dataset is unusable"), List.of(field),
                    sprintFindingCodes(result, sprintId, field));
        }
        if (value == null) {
            return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                    List.of("The source field is null"), List.of(field),
                    sprintFindingCodes(result, sprintId, field));
        }
        List<String> codes = sprintFindingCodes(result, sprintId, field);
        if (!codes.isEmpty()) {
            return new MetricResult<>(value, MetricAvailability.AVAILABLE_WITH_WARNINGS,
                    List.of("Relevant validation findings affect the source-fact interpretation"),
                    List.of(field), codes);
        }
        return new MetricResult<>(value, MetricAvailability.AVAILABLE, List.of(), List.of(field), List.of());
    }

    private MetricResult<Integer> distinctIssueCount(LoadResult result, String sprintId) {
        if (result.usabilityState() == UsabilityState.UNUSABLE) {
            return new MetricResult<>(null, MetricAvailability.UNAVAILABLE,
                    List.of("The loaded dataset is unusable"),
                    List.of("IssueSprintLink.sprintId", "IssueSprintLink.issueKey"),
                    findingCodes(result, null));
        }

        boolean sprintExists = result.sprints().stream().anyMatch(sprint -> sprintId.equals(sprint.sprintId()));
        if (!sprintExists) {
            return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                    List.of("Sprint not found in the loaded sprint records"),
                    List.of("IssueSprintLink.sprintId", "IssueSprintLink.issueKey"),
                    membershipFindingCodes(result, sprintId));
        }

        Set<String> knownIssueKeys = new LinkedHashSet<>();
        for (Issue issue : result.issues()) {
            if (issue.issueKey() != null) knownIssueKeys.add(issue.issueKey());
        }

        Set<String> distinctKeys = new LinkedHashSet<>();
        Set<String> pairs = new LinkedHashSet<>();
        boolean duplicatePair = false;
        for (IssueSprintLink link : result.issueSprintLinks()) {
            if (!sprintId.equals(link.sprintId()) || link.issueKey() == null) continue;
            if (!knownIssueKeys.contains(link.issueKey())) continue;
            String pair = sprintId + "\u0000" + link.issueKey();
            if (!pairs.add(pair)) duplicatePair = true;
            distinctKeys.add(link.issueKey());
        }

        List<String> codes = membershipFindingCodes(result, sprintId);
        boolean incompleteCoverage = codes.stream().anyMatch(code -> code.equals("UNKNOWN_ISSUE_REFERENCE"));
        if (distinctKeys.isEmpty()) {
            List<String> reasons = incompleteCoverage
                    ? List.of("Membership coverage is incomplete because unknown Issue references were excluded")
                    : List.of("No IssueSprintLink membership rows were found for the sprint");
            return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                    reasons,
                    List.of("IssueSprintLink.sprintId", "IssueSprintLink.issueKey", "Issue.issueKey"),
                    codes);
        }

        MetricAvailability availability = !codes.isEmpty() || duplicatePair
                ? MetricAvailability.AVAILABLE_WITH_WARNINGS
                : MetricAvailability.AVAILABLE;
        List<String> reasons = duplicatePair
                ? List.of("Repeated sprint/issue link rows were de-duplicated")
                : List.of();
        return new MetricResult<>(distinctKeys.size(), availability, reasons,
                List.of("IssueSprintLink.sprintId", "IssueSprintLink.issueKey", "Issue.issueKey"),
                codes);
    }

    private MetricResult<AssigneeDistribution> assigneeDistribution(LoadResult result, String sprintId) {
        if (result.usabilityState() == UsabilityState.UNUSABLE) {
            return new MetricResult<>(null, MetricAvailability.UNAVAILABLE,
                    List.of("The loaded dataset is unusable"),
                    List.of("IssueSprintLink.assignee", "Issue.assignee"),
                    findingCodes(result, null));
        }

        boolean sprintExists = result.sprints().stream().anyMatch(sprint -> sprintId.equals(sprint.sprintId()));
        if (!sprintExists) {
            return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                    List.of("Sprint not found in the loaded sprint records"),
                    List.of("IssueSprintLink.assignee", "Issue.assignee"),
                    membershipFindingCodes(result, sprintId));
        }

        Map<String, Issue> knownIssues = new LinkedHashMap<>();
        for (Issue issue : result.issues()) {
            if (issue.issueKey() != null) knownIssues.put(issue.issueKey(), issue);
        }

        Map<String, List<IssueSprintLink>> linksByIssue = new LinkedHashMap<>();
        for (IssueSprintLink link : result.issueSprintLinks()) {
            if (!sprintId.equals(link.sprintId()) || link.issueKey() == null) continue;
            if (!knownIssues.containsKey(link.issueKey())) continue;
            linksByIssue.computeIfAbsent(link.issueKey(), ignored -> new ArrayList<>()).add(link);
        }

        if (linksByIssue.isEmpty()) {
            List<String> codes = membershipFindingCodes(result, sprintId);
            boolean incompleteCoverage = codes.contains("UNKNOWN_ISSUE_REFERENCE");
            List<String> reasons = incompleteCoverage
                    ? List.of("Assignee coverage is incomplete because unknown Issue references were excluded")
                    : List.of("No known Issue membership rows were found for the sprint");
            return new MetricResult<>(null, MetricAvailability.INSUFFICIENT_DATA,
                    reasons,
                    List.of("IssueSprintLink.assignee", "Issue.assignee"),
                    codes);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<AssigneeConflict> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<IssueSprintLink>> entry : linksByIssue.entrySet()) {
            String issueKey = entry.getKey();
            Issue issue = knownIssues.get(issueKey);
            Set<String> linkAssignees = new LinkedHashSet<>();
            Set<String> observations = new LinkedHashSet<>();
            for (IssueSprintLink link : entry.getValue()) {
                if (link.assignee() != null) {
                    linkAssignees.add(link.assignee());
                    observations.add(link.assignee());
                }
            }
            if (issue.assignee() != null) observations.add(issue.assignee());

            String resolved;
            if (linkAssignees.size() > 1) {
                resolved = null;
            } else if (!linkAssignees.isEmpty()) {
                resolved = linkAssignees.iterator().next();
            } else {
                resolved = issue.assignee();
            }

            if (linkAssignees.size() > 1 || observations.size() > 1 || resolved == null) {
                conflicts.add(new AssigneeConflict(issueKey, resolved,
                        observations.stream().sorted().toList()));
            }
            if (resolved != null) counts.merge(resolved, 1, Integer::sum);
        }

        Map<String, Integer> sortedCounts = new LinkedHashMap<>();
        for (String assignee : new TreeSet<>(counts.keySet())) {
            sortedCounts.put(assignee, counts.get(assignee));
        }

        List<String> codes = membershipFindingCodes(result, sprintId);
        List<String> reasons = new ArrayList<>();
        if (!conflicts.isEmpty()) {
            reasons.add("Missing or conflicting assignee observations were preserved as evidence");
        }
        MetricAvailability availability = !codes.isEmpty() || !conflicts.isEmpty()
                ? MetricAvailability.AVAILABLE_WITH_WARNINGS
                : MetricAvailability.AVAILABLE;
        return new MetricResult<>(new AssigneeDistribution(sortedCounts, conflicts),
                availability, reasons,
                List.of("IssueSprintLink.assignee", "Issue.assignee", "IssueSprintLink.issueKey"),
                codes);
    }

    private MetricResult<Integer> unknownIssueReferenceCount(LoadResult result, String sprintId) {
        Set<String> knownKeys = new LinkedHashSet<>();
        for (Issue issue : result.issues()) if (issue.issueKey() != null) knownKeys.add(issue.issueKey());
        int count = 0;
        for (IssueSprintLink link : result.issueSprintLinks()) {
            if (sprintId.equals(link.sprintId())
                    && link.issueKey() != null
                    && !knownKeys.contains(link.issueKey())) count++;
        }
        if (result.usabilityState() == UsabilityState.UNUSABLE) {
            return new MetricResult<>(null, MetricAvailability.UNAVAILABLE,
                    List.of("The loaded dataset is unusable"),
                    List.of("IssueSprintLink.issueKey", "Issue.issueKey"),
                    findingCodes(result, null));
        }
        if (count > 0) {
            return new MetricResult<>(count, MetricAvailability.AVAILABLE_WITH_WARNINGS,
                    List.of("Unknown Issue master references were preserved but excluded"),
                    List.of("IssueSprintLink.issueKey", "Issue.issueKey"),
                    List.of("UNKNOWN_ISSUE_REFERENCE"));
        }
        return new MetricResult<>(0, MetricAvailability.AVAILABLE,
                List.of(), List.of("IssueSprintLink.issueKey", "Issue.issueKey"), List.of());
    }

    private MetricResult<Integer> quarantinedIssueRecordCount(LoadResult result) {
        int count = (int) result.quarantinedRecords().stream()
                .filter(record -> record.sourceRef().fileRole() == FileRole.ISSUES)
                .count();
        if (result.usabilityState() == UsabilityState.UNUSABLE) {
            return new MetricResult<>(null, MetricAvailability.UNAVAILABLE,
                    List.of("The loaded dataset is unusable"),
                    List.of("Issue.sourceRef"), findingCodes(result, null));
        }
        if (count > 0) {
            return new MetricResult<>(count, MetricAvailability.AVAILABLE_WITH_WARNINGS,
                    List.of("Duplicate or invalid Issue master rows were quarantined"),
                    List.of("Issue.sourceRef"), List.of("DUPLICATE_MASTER_CONFLICT"));
        }
        return new MetricResult<>(0, MetricAvailability.AVAILABLE,
                List.of(), List.of("Issue.sourceRef"), List.of());
    }

    private List<String> findingCodes(LoadResult result, FileRole role) {
        return result.findings().stream()
                .filter(finding -> role == null || finding.fileRole() == role)
                .map(ValidationFinding::errorCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> sprintFindingCodes(LoadResult result, String sprintId, String field) {
        return result.findings().stream()
                .filter(finding -> finding.fileRole() == FileRole.SPRINTS)
                .filter(finding -> finding.recordNumber() == null
                        || sprintSourceFindingMatches(result, sprintId, finding))
                .map(ValidationFinding::errorCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private boolean sprintSourceFindingMatches(LoadResult result, String sprintId,
                                               ValidationFinding finding) {
        boolean rowMapsToRetainedSprint = false;
        for (Sprint sprint : result.sprints()) {
            if (sprint.sourceRef() != null
                    && finding.recordNumber() != null
                    && finding.recordNumber().equals(sprint.sourceRef().recordNumber())) {
                rowMapsToRetainedSprint = true;
                if (sprintId == null || sprintId.equals(sprint.sprintId())) return true;
            }
        }

        // A finding tied to a row that no longer maps to a retained sprint cannot be
        // assigned safely. Preserve it as dataset-level evidence for every sprint.
        return !rowMapsToRetainedSprint;
    }

    private List<String> membershipFindingCodes(LoadResult result, String sprintId) {
        return result.findings().stream()
                .filter(finding -> finding.errorCode().equals("UNKNOWN_ISSUE_REFERENCE")
                        || finding.errorCode().equals("DUPLICATE_LINK_PAIR"))
                .filter(finding -> membershipFindingMatches(result, sprintId, finding))
                .map(ValidationFinding::errorCode)
                .distinct()
                .sorted()
                .toList();
    }

    private boolean membershipFindingMatches(LoadResult result, String sprintId,
                                             ValidationFinding finding) {
        if (finding.recordNumber() == null) return true;

        boolean matchingLinkFound = false;
        for (IssueSprintLink link : result.issueSprintLinks()) {
            if (link.sourceRef() != null
                    && link.sourceRef().fileRole() == FileRole.ISSUE_SPRINT_SUMMARY
                    && finding.recordNumber().equals(link.sourceRef().recordNumber())) {
                matchingLinkFound = true;
                if (sprintId.equals(link.sprintId())) return true;
            }
        }

        // Preserve evidence when a source row cannot be associated confidently.
        return !matchingLinkFound;
    }
}
