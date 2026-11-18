package com.ablmvp.sprintassistant.api.dto;

import java.util.List;

public record TrendResponse(
        List<TrendObservationResponse> observations,
        String ordering,
        List<String> limitations) {
}
