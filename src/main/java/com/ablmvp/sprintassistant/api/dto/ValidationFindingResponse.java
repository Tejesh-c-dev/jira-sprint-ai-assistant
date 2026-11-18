package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.data.result.FindingSeverity;
import com.ablmvp.sprintassistant.domain.FileRole;

public record ValidationFindingResponse(
        FileRole fileRole,
        Integer recordNumber,
        String field,
        FindingSeverity severity,
        String errorCode,
        String reason,
        String rawValue,
        boolean blocking) {
}
