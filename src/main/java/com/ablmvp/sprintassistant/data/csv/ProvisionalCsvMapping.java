package com.ablmvp.sprintassistant.data.csv;

import java.util.List;

/**
 * Synthetic/provisional header contract. These names are not verified upstream headers and are
 * intentionally isolated so a later profiled mapping can replace this class without changing the
 * domain model or loader orchestration.
 */
public final class ProvisionalCsvMapping {
    private ProvisionalCsvMapping() {
    }

    public static final List<String> ISSUE_HEADERS = List.of(
            "key", "summary", "issueType", "status", "storyPoint", "priority", "assignee",
            "description", "creator", "reporter", "watchCount", "commentCount", "votes",
            "fixVersions", "affectedVersions", "issueLinks", "blockedBy", "blocks",
            "dependedOnBy", "dependedOn", "subtasks");

    public static final List<String> LINK_HEADERS = List.of(
            "sprintId", "issueKey", "storyId", "status", "assignee", "priorityId", "summary",
            "initialStoryPoint", "currentStoryPoint", "projectID");

    public static final List<String> SPRINT_HEADERS = List.of(
            "sprintId", "sprintName", "sprintState", "sprintStartDate", "sprintEndDate",
            "sprintCompleteDate", "SprintLength", "NoOfDevelopers", "totalNumberOfIssues",
            "completedIssuesCount", "issuesNotCompletedInCurrentSprint", "puntedIssues",
            "issuesCompletedInAnotherSprint", "issueKeysAddedDuringSprint",
            "completedIssuesInitialEstimateSum", "completedIssuesEstimateSum",
            "puntedIssuesInitialEstimateSum", "puntedIssuesEstimateSum",
            "issuesNotCompletedInitialEstimateSum", "issuesNotCompletedEstimateSum",
            "issuesCompletedInAnotherSprintInitialEstimateSum",
            "issuesCompletedInAnotherSprintEstimateSum");

    public static final String FIXTURE_DATE_FORMAT = "yyyy-MM-dd";
}
