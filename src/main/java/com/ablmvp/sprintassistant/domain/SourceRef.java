package com.ablmvp.sprintassistant.domain;

public record SourceRef(FileRole fileRole, int recordNumber, Integer rawLineNumber) {
    public SourceRef {
        if (fileRole == null) throw new IllegalArgumentException("fileRole must not be null");
        if (recordNumber < 1) throw new IllegalArgumentException("recordNumber must be positive");
        if (rawLineNumber != null && rawLineNumber < 1) {
            throw new IllegalArgumentException("rawLineNumber must be positive when present");
        }
    }
}
