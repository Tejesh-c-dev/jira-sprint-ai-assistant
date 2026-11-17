package com.ablmvp.sprintassistant.data.result;

import com.ablmvp.sprintassistant.domain.SourceRef;

public record QuarantinedRecord(SourceRef sourceRef, String reason, String linkId) {
    public QuarantinedRecord(SourceRef sourceRef, String reason) {
        this(sourceRef, reason, null);
    }
}
