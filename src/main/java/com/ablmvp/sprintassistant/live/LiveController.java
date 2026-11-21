package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.live.model.EmployeeWorkSignal;
import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/live")
public class LiveController {
    private final LiveSprintCache cache;
    private final LiveSprintRefreshService refresh;
    private final LiveMetricsService metrics;

    public LiveController(LiveSprintCache cache, LiveSprintRefreshService refresh, LiveMetricsService metrics) {
        this.cache = cache;
        this.refresh = refresh;
        this.metrics = metrics;
    }

    @GetMapping("/sprint") public LiveSprintSnapshot sprint() { return cache.get(); }
    @GetMapping("/metrics") public Object metrics() { return metrics.metrics(cache.get()); }
    @GetMapping("/team") public TeamResponse team() {
        return new TeamResponse(cache.get().lastSyncedAt(), metrics.team(cache.get()));
    }
    @PostMapping("/refresh") public LiveSprintSnapshot refresh() { return refresh.refresh(); }
    @GetMapping("/status") public StatusResponse status() {
        LiveSprintSnapshot current = cache.get();
        return new StatusResponse(current.dataSourceStatus(), current.lastSyncedAt(), current.warning(), current.issues().size(), Instant.now());
    }

    public record TeamResponse(Instant lastSyncedAt, List<EmployeeWorkSignal> members) { }
    public record StatusResponse(String status, Instant lastSyncedAt, String warning, int issueCount, Instant checkedAt) { }
}
