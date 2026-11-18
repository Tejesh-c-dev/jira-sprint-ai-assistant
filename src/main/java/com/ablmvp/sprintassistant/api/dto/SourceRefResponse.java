package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.domain.FileRole;

public record SourceRefResponse(FileRole fileRole, int recordNumber, Integer rawLineNumber) {
}
