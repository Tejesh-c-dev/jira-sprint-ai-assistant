package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.metrics.MetricAvailability;
import java.time.LocalDate;

public record SprintComparisonItemResponse(
        String sprintId,
        String sprintName,
        LocalDate startDate,
        Integer recordedIssueCount,
        MetricAvailability recordedIssueCountAvailability,
        Integer distinctKnownIssueCount,
        MetricAvailability distinctKnownIssueCountAvailability,
        Integer unknownIssueReferenceCount,
        MetricAvailability unknownIssueReferenceCountAvailability) {
}
