package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.metrics.HealthDimensionStatus;
import java.util.List;

public record HealthDimensionResponse(
        String dimension,
        HealthDimensionStatus status,
        String explanation,
        List<String> evidence,
        List<String> warnings,
        String reason) {
}
