package com.ablmvp.sprintassistant.domain;

import java.time.Instant;
import java.util.List;

public record DatasetProvenance(
        String datasetName,
        DataKind dataKind,
        String attribution,
        String sourceRepository,
        String licenceStatus,
        String snapshotLabel,
        List<String> knownLimitations,
        Instant loadedAt,
        List<FileFingerprint> fileFingerprints) {

    public DatasetProvenance {
        knownLimitations = knownLimitations == null ? List.of() : List.copyOf(knownLimitations);
        fileFingerprints = fileFingerprints == null ? List.of() : List.copyOf(fileFingerprints);
    }
}
