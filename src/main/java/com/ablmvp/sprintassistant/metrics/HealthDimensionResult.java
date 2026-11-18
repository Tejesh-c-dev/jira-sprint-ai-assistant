package com.ablmvp.sprintassistant.metrics;

import java.util.List;

public record HealthDimensionResult(
        String dimension,
        HealthDimensionStatus status,
        String explanation,
        List<String> evidence,
        List<String> warnings,
        String reason) {

    public HealthDimensionResult {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
