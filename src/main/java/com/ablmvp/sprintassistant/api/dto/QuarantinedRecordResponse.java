package com.ablmvp.sprintassistant.api.dto;

public record QuarantinedRecordResponse(
        SourceRefResponse sourceRef,
        String reason,
        String linkId) {
}
