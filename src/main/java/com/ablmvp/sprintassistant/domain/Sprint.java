package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Sprint(
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
        SprintEstimateSums estimateSums,
        SourceRef sourceRef) {

    public Sprint {
        recordedAddedIssueKeys = recordedAddedIssueKeys == null ? null : List.copyOf(recordedAddedIssueKeys);
    }
}
