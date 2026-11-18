package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.metrics.MetricAvailability;
import java.time.LocalDate;

public record TrendObservationResponse(
        String sprintId,
        String sprintName,
        LocalDate startDate,
        LocalDate endDate,
        Integer recordedIssueCount,
        MetricAvailability recordedIssueCountAvailability,
        Integer recordedCompletedCount,
        MetricAvailability recordedCompletedCountAvailability,
        Integer recordedPuntedCount,
        MetricAvailability recordedPuntedCountAvailability,
        Integer lengthDays) {
}
