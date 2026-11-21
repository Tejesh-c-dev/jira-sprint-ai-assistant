package com.ablmvp.sprintassistant.live.model;

import java.time.Instant;
import java.util.List;

public record LiveSprintSnapshot(SprintInfo sprint, List<SprintIssue> issues, Instant lastSyncedAt,
                                 String dataSourceStatus, String warning) {
    public LiveSprintSnapshot { issues = issues == null ? List.of() : List.copyOf(issues); }
    public static LiveSprintSnapshot empty(String status, String warning) {
        return new LiveSprintSnapshot(null, List.of(), null, status, warning);
    }
}
