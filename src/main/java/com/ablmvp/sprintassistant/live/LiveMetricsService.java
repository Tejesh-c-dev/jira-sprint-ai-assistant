package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.live.model.EmployeeWorkSignal;
import com.ablmvp.sprintassistant.live.model.LiveMetrics;
import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import com.ablmvp.sprintassistant.live.model.SprintIssue;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class LiveMetricsService {
    public LiveMetrics metrics(LiveSprintSnapshot snapshot) {
        List<SprintIssue> issues = snapshot.issues();
        int completed = (int) issues.stream().filter(SprintIssue::completed).count();
        int inProgress = (int) issues.stream().filter(SprintIssue::inProgress).filter(SprintIssue::remaining).count();
        int blocked = (int) issues.stream().filter(SprintIssue::blocked).count();
        int overdue = (int) issues.stream().filter(i -> i.overdue(LocalDate.now())).count();
        int remaining = issues.size() - completed;
        double donePoints = issues.stream().filter(SprintIssue::completed).mapToDouble(i -> value(i.storyPoints())).sum();
        double remainingPoints = issues.stream().filter(SprintIssue::remaining).mapToDouble(i -> value(i.storyPoints())).sum();
        int unassigned = (int) issues.stream().filter(i -> "Unassigned".equals(i.assignee())).count();
        List<String> evidence = new ArrayList<>();
        if (remaining > 0) evidence.add(remaining + " of " + issues.size() + " issues remain incomplete");
        if (blocked > 0) evidence.add(blocked + (blocked == 1 ? " issue is blocked" : " issues are blocked"));
        if (overdue > 0) evidence.add(overdue + (overdue == 1 ? " issue is overdue" : " issues are overdue"));
        if (issues.isEmpty()) evidence.add("No issues are present in the active sprint snapshot");
        String health = issues.isEmpty() ? "UNKNOWN" : blocked >= 3 || overdue >= 3 ? "CRITICAL" : blocked > 0 || overdue > 0 ? "AT_RISK" : "HEALTHY";
        var sprint = snapshot.sprint();
        return new LiveMetrics(sprint == null ? null : sprint.id(), sprint == null ? null : sprint.name(),
                sprint == null ? null : sprint.startDate(), sprint == null ? null : sprint.endDate(), issues.size(), completed,
                inProgress, remaining, donePoints, remainingPoints, blocked, overdue, unassigned,
                snapshot.lastSyncedAt(), snapshot.dataSourceStatus(), snapshot.warning(), health, evidence);
    }

    public List<EmployeeWorkSignal> team(LiveSprintSnapshot snapshot) {
        Map<String, List<SprintIssue>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        snapshot.issues().stream().filter(i -> !"Unassigned".equals(i.assignee()))
                .forEach(i -> groups.computeIfAbsent(i.assignee(), ignored -> new ArrayList<>()).add(i));
        double averageRemaining = groups.values().stream()
                .mapToInt(group -> (int) group.stream().filter(SprintIssue::remaining).count()).average().orElse(0);
        List<EmployeeWorkSignal> signals = new ArrayList<>();
        groups.forEach((assignee, issues) -> {
            int completed = (int) issues.stream().filter(SprintIssue::completed).count();
            int remaining = issues.size() - completed;
            int wip = (int) issues.stream().filter(SprintIssue::inProgress).filter(SprintIssue::remaining).count();
            int blocked = (int) issues.stream().filter(SprintIssue::blocked).count();
            int overdue = (int) issues.stream().filter(i -> i.overdue(LocalDate.now())).count();
            double completedPoints = issues.stream().filter(SprintIssue::completed).mapToDouble(i -> value(i.storyPoints())).sum();
            double remainingPoints = issues.stream().filter(SprintIssue::remaining).mapToDouble(i -> value(i.storyPoints())).sum();
            boolean highRemaining = averageRemaining > 0 && remaining > Math.max(3, averageRemaining * 1.5);
            boolean highWip = wip >= 3;
            List<String> reasons = new ArrayList<>();
            if (blocked > 0) reasons.add(blocked + (blocked == 1 ? " assigned issue is blocked" : " assigned issues are blocked"));
            if (overdue > 0) reasons.add(overdue + (overdue == 1 ? " assigned issue is overdue" : " assigned issues are overdue"));
            if (highRemaining) reasons.add("Remaining issue count is high relative to the team average");
            if (highWip) reasons.add("Three or more issues are in progress");
            String status = blocked > 0 ? "BLOCKED" : highRemaining || highWip ? "OVERLOADED" : overdue > 0 ? "NEEDS_ATTENTION" : "OK";
            if (reasons.isEmpty()) reasons.add("No open support signals from the current snapshot");
            signals.add(new EmployeeWorkSignal(assignee, issues.size(), completed, remaining,
                    (int) issues.stream().filter(SprintIssue::inProgress).count(), completedPoints, remainingPoints,
                    blocked, overdue, wip, status, reasons));
        });
        return signals;
    }

    private double value(Double points) { return points == null ? 0 : points; }
}
