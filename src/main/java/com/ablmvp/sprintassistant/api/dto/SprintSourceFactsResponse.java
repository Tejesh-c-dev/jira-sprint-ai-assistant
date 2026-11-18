package com.ablmvp.sprintassistant.api.dto;

public record SprintSourceFactsResponse(
        MetricResultResponse<Integer> recordedIssueCount,
        MetricResultResponse<Integer> recordedCompletedCount,
        MetricResultResponse<Integer> recordedNotCompletedCount,
        MetricResultResponse<Integer> recordedPuntedCount,
        MetricResultResponse<Integer> recordedCompletedElsewhereCount,
        MetricResultResponse<Integer> recordedSprintLength,
        MetricResultResponse<java.util.List<String>> recordedAddedIssueKeys,
        MetricResultResponse<java.math.BigDecimal> completedIssuesInitialEstimateSum,
        MetricResultResponse<java.math.BigDecimal> completedIssuesCurrentEstimateSum,
        MetricResultResponse<java.math.BigDecimal> puntedIssuesInitialEstimateSum,
        MetricResultResponse<java.math.BigDecimal> puntedIssuesCurrentEstimateSum,
        MetricResultResponse<java.math.BigDecimal> notCompletedIssuesInitialEstimateSum,
        MetricResultResponse<java.math.BigDecimal> notCompletedIssuesCurrentEstimateSum,
        MetricResultResponse<java.math.BigDecimal> completedElsewhereInitialEstimateSum,
        MetricResultResponse<java.math.BigDecimal> completedElsewhereCurrentEstimateSum) {
}
