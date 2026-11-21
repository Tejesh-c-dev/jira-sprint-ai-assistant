package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.config.AppProperties;
import com.ablmvp.sprintassistant.live.jira.JiraSprintService;
import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LiveSprintRefreshService {
    private static final Logger log = LoggerFactory.getLogger(LiveSprintRefreshService.class);
    private final JiraSprintService jira;
    private final LiveSprintCache cache;
    private final AppProperties properties;

    public LiveSprintRefreshService(JiraSprintService jira, LiveSprintCache cache, AppProperties properties) {
        this.jira = jira;
        this.cache = cache;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialRefresh() { if (isLive()) refresh(); }

    @Scheduled(fixedDelayString = "${app.jira.poll-interval-ms:60000}", initialDelayString = "${app.jira.poll-interval-ms:60000}")
    public void scheduledRefresh() { if (isLive()) refresh(); }

    public LiveSprintSnapshot refresh() {
        if (!isLive()) return cache.get();
        try {
            LiveSprintSnapshot fresh = jira.fetchActiveSprint();
            cache.set(fresh);
        } catch (Exception error) {
            log.warn("Jira sprint refresh failed: {}", error.getMessage());
            LiveSprintSnapshot previous = cache.get();
            cache.set(new LiveSprintSnapshot(previous.sprint(), previous.issues(), previous.lastSyncedAt(),
                    "UNAVAILABLE", error.getMessage() == null ? "Jira refresh failed." : error.getMessage()));
        }
        return cache.get();
    }

    private boolean isLive() { return "live".equalsIgnoreCase(properties.getMode()); }
}
