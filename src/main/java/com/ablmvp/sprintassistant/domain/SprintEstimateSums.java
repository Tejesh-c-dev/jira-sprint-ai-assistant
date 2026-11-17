package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;

public record SprintEstimateSums(
        BigDecimal completedInitial,
        BigDecimal completedCurrent,
        BigDecimal puntedInitial,
        BigDecimal puntedCurrent,
        BigDecimal notCompletedInitial,
        BigDecimal notCompletedCurrent,
        BigDecimal completedElsewhereInitial,
        BigDecimal completedElsewhereCurrent) {
}
