package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.domain.FileRole;

public record FileFingerprintResponse(
        FileRole fileRole,
        String fileName,
        long byteSize,
        String sha256) {
}
