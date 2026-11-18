package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.Sprint;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SprintRepository {
    private final LoadResult loadResult;

    public SprintRepository(LoadResult loadResult) {
        this.loadResult = loadResult;
    }

    public List<Sprint> findAll() {
        return loadResult.sprints().stream()
                .sorted(Comparator.comparing(Sprint::sprintId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Optional<Sprint> findById(String sprintId) {
        return loadResult.sprints().stream()
                .filter(sprint -> sprintId.equals(sprint.sprintId()))
                .findFirst();
    }

    public LoadResult loadResult() {
        return loadResult;
    }
}
