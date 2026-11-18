package com.ablmvp.sprintassistant.api.dto;

import java.util.List;

public record AssigneeConflictResponse(
        String issueKey,
        String resolvedAssignee,
        List<String> observedAssignees) {
}
