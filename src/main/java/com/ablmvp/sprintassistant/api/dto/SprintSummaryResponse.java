package com.ablmvp.sprintassistant.api.dto;

import java.time.LocalDate;

public record SprintSummaryResponse(
        String sprintId,
        String name,
        String state,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate completeDate,
        Integer lengthDays) {
}
