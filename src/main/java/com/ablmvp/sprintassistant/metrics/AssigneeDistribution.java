package com.ablmvp.sprintassistant.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AssigneeDistribution(
        Map<String, Integer> issueCounts,
        List<AssigneeConflict> conflicts) {

    public AssigneeDistribution {
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (issueCounts != null) copy.putAll(issueCounts);
        issueCounts = Collections.unmodifiableMap(copy);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }
}
