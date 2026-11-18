package com.ablmvp.sprintassistant.metrics;

import java.util.List;
import java.util.Objects;

public record MetricResult<T>(
        T value,
        MetricAvailability availability,
        List<String> reasons,
        List<String> sourceFields,
        List<String> findingCodes) {

    public MetricResult {
        Objects.requireNonNull(availability, "availability must not be null");
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        sourceFields = sourceFields == null ? List.of() : List.copyOf(sourceFields);
        findingCodes = findingCodes == null ? List.of() : List.copyOf(findingCodes);
    }
}
