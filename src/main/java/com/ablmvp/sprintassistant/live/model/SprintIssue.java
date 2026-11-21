package com.ablmvp.sprintassistant.live.model;

import java.time.LocalDate;

public record SprintIssue(String key, String summary, String status, String statusCategory, String assignee,
                          Double storyPoints, String priority, LocalDate dueDate, boolean blocked, String blockerReason) {
    public boolean completed() {
        return "done".equalsIgnoreCase(statusCategory) || status != null &&
                (status.equalsIgnoreCase("done") || status.equalsIgnoreCase("closed") || status.equalsIgnoreCase("resolved"));
    }
    public boolean inProgress() {
        return "indeterminate".equalsIgnoreCase(statusCategory) || status != null &&
                (status.toLowerCase().contains("progress") || status.toLowerCase().contains("development") || status.toLowerCase().contains("review"));
    }
    public boolean remaining() { return !completed(); }
    public boolean overdue(LocalDate today) { return dueDate != null && dueDate.isBefore(today) && remaining(); }
}
