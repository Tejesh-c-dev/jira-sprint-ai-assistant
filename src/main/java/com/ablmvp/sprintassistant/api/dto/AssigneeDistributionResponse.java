package com.ablmvp.sprintassistant.api.dto;

import java.util.Map;
import java.util.List;

public record AssigneeDistributionResponse(
        Map<String, Integer> issueCounts,
        List<AssigneeConflictResponse> conflicts) {
}
