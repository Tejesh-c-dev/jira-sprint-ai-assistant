package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.metrics.HealthStatus;
import java.util.List;

public record SprintHealthResponse(
        String sprintId,
        HealthStatus overallStatus,
        List<HealthDimensionResponse> dimensions,
        List<String> explanation,
        List<String> warnings) {
}
