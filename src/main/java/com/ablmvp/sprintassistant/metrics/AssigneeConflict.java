package com.ablmvp.sprintassistant.metrics;

import java.util.List;

public record AssigneeConflict(
        String issueKey,
        String resolvedAssignee,
        List<String> observedAssignees) {

    public AssigneeConflict {
        observedAssignees = observedAssignees == null ? List.of() : List.copyOf(observedAssignees);
    }
}
