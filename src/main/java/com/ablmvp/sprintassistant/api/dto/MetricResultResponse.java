package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.metrics.MetricAvailability;
import java.util.List;

public record MetricResultResponse<T>(
        T value,
        MetricAvailability availability,
        List<String> reasons,
        List<String> sourceFields,
        List<String> findingCodes) {
}
