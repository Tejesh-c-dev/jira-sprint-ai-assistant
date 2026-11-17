package com.ablmvp.sprintassistant.domain;

public record FileFingerprint(FileRole fileRole, String fileName, long byteSize, String sha256) {
    public FileFingerprint {
        if (fileRole == null || fileName == null || fileName.isBlank() || sha256 == null || sha256.isBlank()) {
            throw new IllegalArgumentException("file fingerprint fields must be present");
        }
        if (byteSize < 0) throw new IllegalArgumentException("byteSize must not be negative");
    }
}
