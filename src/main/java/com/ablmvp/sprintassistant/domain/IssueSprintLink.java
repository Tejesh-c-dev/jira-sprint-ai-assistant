package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;

public record IssueSprintLink(
        String linkId,
        String sprintId,
        String issueKey,
        String storyId,
        String status,
        String assignee,
        String priorityId,
        String summary,
        BigDecimal initialStoryPoint,
        BigDecimal currentStoryPoint,
        String projectId,
        SourceRef sourceRef) {
}
