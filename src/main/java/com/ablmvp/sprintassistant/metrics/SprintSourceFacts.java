package com.ablmvp.sprintassistant.metrics;

import java.math.BigDecimal;
import java.util.List;

public record SprintSourceFacts(
        String sprintId,
        MetricResult<Integer> recordedIssueCount,
        MetricResult<Integer> recordedCompletedCount,
        MetricResult<Integer> recordedNotCompletedCount,
        MetricResult<Integer> recordedPuntedCount,
        MetricResult<Integer> recordedCompletedElsewhereCount,
        MetricResult<Integer> recordedSprintLength,
        MetricResult<List<String>> recordedAddedIssueKeys,
        MetricResult<BigDecimal> completedIssuesInitialEstimateSum,
        MetricResult<BigDecimal> completedIssuesCurrentEstimateSum,
        MetricResult<BigDecimal> puntedIssuesInitialEstimateSum,
        MetricResult<BigDecimal> puntedIssuesCurrentEstimateSum,
        MetricResult<BigDecimal> notCompletedIssuesInitialEstimateSum,
        MetricResult<BigDecimal> notCompletedIssuesCurrentEstimateSum,
        MetricResult<BigDecimal> completedElsewhereInitialEstimateSum,
        MetricResult<BigDecimal> completedElsewhereCurrentEstimateSum) {
}
