package com.ablmvp.sprintassistant.api.dto;

import java.math.BigDecimal;

public record SprintEstimateSumsResponse(
        BigDecimal completedInitial,
        BigDecimal completedCurrent,
        BigDecimal puntedInitial,
        BigDecimal puntedCurrent,
        BigDecimal notCompletedInitial,
        BigDecimal notCompletedCurrent,
        BigDecimal completedElsewhereInitial,
        BigDecimal completedElsewhereCurrent) {
}
