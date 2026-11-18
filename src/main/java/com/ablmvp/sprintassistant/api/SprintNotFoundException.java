package com.ablmvp.sprintassistant.api;

public class SprintNotFoundException extends RuntimeException {
    private final String sprintId;

    public SprintNotFoundException(String sprintId) {
        super("Sprint not found: " + sprintId);
        this.sprintId = sprintId;
    }

    public String sprintId() {
        return sprintId;
    }
}
