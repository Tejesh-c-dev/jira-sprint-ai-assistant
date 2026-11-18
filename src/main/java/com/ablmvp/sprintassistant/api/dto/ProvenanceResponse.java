package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.domain.DataKind;
import java.time.Instant;
import java.util.List;

public record ProvenanceResponse(
        String datasetName,
        DataKind dataKind,
        String attribution,
        String sourceRepository,
        String licenceStatus,
        String snapshotLabel,
        List<String> knownLimitations,
        Instant loadedAt,
        List<FileFingerprintResponse> fileFingerprints) {
}
