package com.ablmvp.sprintassistant.api.dto;

import java.util.List;

public record SprintComparisonResponse(
        List<SprintComparisonItemResponse> sprints,
        List<String> limitations) {
}
