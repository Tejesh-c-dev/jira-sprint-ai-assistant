package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class LiveSprintCache {
    private final AtomicReference<LiveSprintSnapshot> snapshot = new AtomicReference<>(
            LiveSprintSnapshot.empty("NOT_SYNCED", "No Jira sprint data has been synchronized yet."));
    public LiveSprintSnapshot get() { return snapshot.get(); }
    public void set(LiveSprintSnapshot value) { snapshot.set(value); }
}
