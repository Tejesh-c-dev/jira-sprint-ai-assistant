package com.ablmvp.sprintassistant.live.model;

import java.time.Instant;
import java.util.List;

public record LiveMetrics(String sprintId, String sprintName, Instant startDate, Instant endDate,
                          int totalIssues, int completedIssues, int inProgressIssues, int remainingIssues,
                          double storyPointsCompleted, double storyPointsRemaining, int blockedIssues,
                          int overdueIssues, int unassignedIssues, Instant lastSyncedAt,
                          String dataSourceStatus, String warning, String health, List<String> evidence) { }
