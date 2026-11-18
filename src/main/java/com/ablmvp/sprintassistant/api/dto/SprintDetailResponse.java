package com.ablmvp.sprintassistant.api.dto;

import java.time.LocalDate;
import java.util.List;

public record SprintDetailResponse(
        String sprintId,
        String name,
        String state,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate completeDate,
        Integer lengthDays,
        Integer developerCount,
        Integer recordedIssueCount,
        Integer recordedCompletedCount,
        Integer recordedNotCompletedCount,
        Integer recordedPuntedCount,
        Integer recordedCompletedElsewhereCount,
        List<String> recordedAddedIssueKeys,
        SprintEstimateSumsResponse estimateSums) {
}
