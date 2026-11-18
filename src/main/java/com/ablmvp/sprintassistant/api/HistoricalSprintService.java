package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.*;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.metrics.MetricResult;
import com.ablmvp.sprintassistant.metrics.SprintMetricsResult;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoricalSprintService {
    private final SprintRepository repository;
    private final SprintMetricsService metricsService;

    public HistoricalSprintService(SprintRepository repository, SprintMetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    public TrendResponse trends() {
        LoadResult result = repository.loadResult();
        List<Sprint> sprints = result.sprints().stream()
                .sorted(Comparator
                        .comparing(Sprint::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Sprint::sprintId, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<TrendObservationResponse> observations = new ArrayList<>();
        for (Sprint sprint : sprints) {
            SprintMetricsResult metrics = metricsService.analyze(result, sprint.sprintId());
            MetricResult<Integer> issueCount = metrics.sourceFacts().recordedIssueCount();
            MetricResult<Integer> completed = metrics.sourceFacts().recordedCompletedCount();
            MetricResult<Integer> punted = metrics.sourceFacts().recordedPuntedCount();
            observations.add(new TrendObservationResponse(
                    sprint.sprintId(), sprint.name(), sprint.startDate(), sprint.endDate(),
                    issueCount.value(), issueCount.availability(),
                    completed.value(), completed.availability(),
                    punted.value(), punted.availability(),
                    sprint.lengthDays()));
        }
        return new TrendResponse(observations,
                "startDate ascending, with missing dates last, then sprintId lexical",
                List.of("No percentage change, carryover, velocity or forecasting is computed."));
    }

    public SprintComparisonResponse compare(List<String> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) {
            return new SprintComparisonResponse(List.of(),
                    List.of("At least one sprintId query parameter is required."));
        }
        LoadResult result = repository.loadResult();
        List<SprintComparisonItemResponse> items = new ArrayList<>();
        for (String sprintId : sprintIds) {
            Sprint sprint = result.sprints().stream()
                    .filter(candidate -> sprintId.equals(candidate.sprintId()))
                    .findFirst()
                    .orElse(null);
            if (sprint == null) continue;
            SprintMetricsResult metrics = metricsService.analyze(result, sprintId);
            MetricResult<Integer> issueCount = metrics.sourceFacts().recordedIssueCount();
            items.add(new SprintComparisonItemResponse(
                    sprint.sprintId(), sprint.name(), sprint.startDate(),
                    issueCount.value(), issueCount.availability(),
                    metrics.distinctIssueCount().value(), metrics.distinctIssueCount().availability(),
                    metrics.unknownIssueReferenceCount().value(),
                    metrics.unknownIssueReferenceCount().availability()));
        }
        return new SprintComparisonResponse(items,
                List.of("Unknown sprint IDs are omitted; source counts and distinct known links remain separate."));
    }
}
