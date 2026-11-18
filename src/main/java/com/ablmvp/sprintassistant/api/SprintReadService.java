package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.SprintDetailResponse;
import com.ablmvp.sprintassistant.api.dto.SprintSummaryResponse;
import com.ablmvp.sprintassistant.api.dto.SprintAnalysisResponse;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SprintReadService {
    private final SprintRepository repository;
    private final SprintMetricsService metricsService;

    public SprintReadService(SprintRepository repository, SprintMetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    public List<SprintSummaryResponse> listSprints() {
        return repository.findAll().stream().map(ApiMapper::toSummary).toList();
    }

    public SprintDetailResponse getSprint(String sprintId) {
        if (sprintId == null || sprintId.isBlank()) {
            throw new InvalidSprintIdException();
        }
        Sprint sprint = repository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));
        return ApiMapper.toDetail(sprint);
    }

    public SprintAnalysisResponse analyzeSprint(String sprintId) {
        getSprint(sprintId);
        return ApiMapper.toAnalysis(metricsService.analyze(repository.loadResult(), sprintId));
    }

    public com.ablmvp.sprintassistant.data.result.LoadResult loadResult() {
        return repository.loadResult();
    }
}
