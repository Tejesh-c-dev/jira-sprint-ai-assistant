package com.ablmvp.sprintassistant.data.result;

import com.ablmvp.sprintassistant.domain.FileRole;

public record ValidationFinding(
        FileRole fileRole,
        Integer recordNumber,
        String field,
        FindingSeverity severity,
        String errorCode,
        String reason,
        String rawValue,
        boolean blocking) {

    public ValidationFinding {
        if (fileRole == null || severity == null || errorCode == null || errorCode.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("finding role, severity, code and reason are required");
        }
    }
}
