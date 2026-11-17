package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;

public record Issue(
        String issueKey,
        String summary,
        String issueType,
        String status,
        BigDecimal storyPoint,
        String priority,
        String assignee,
        String description,
        String creator,
        String reporter,
        String watchCount,
        String commentCount,
        String votes,
        String fixVersions,
        String affectedVersions,
        String issueLinks,
        String blockedBy,
        String blocks,
        String dependedOnBy,
        String dependedOn,
        String subtasks,
        SourceRef sourceRef) {
}
