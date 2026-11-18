package com.ablmvp.sprintassistant.metrics;

import java.util.List;

public record SprintHealthResult(
        String sprintId,
        HealthStatus overallStatus,
        List<HealthDimensionResult> dimensions,
        List<String> explanation,
        List<String> warnings) {

    public SprintHealthResult {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        explanation = explanation == null ? List.of() : List.copyOf(explanation);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
