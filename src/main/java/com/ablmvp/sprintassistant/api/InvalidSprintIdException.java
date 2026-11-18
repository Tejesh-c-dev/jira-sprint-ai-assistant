package com.ablmvp.sprintassistant.api;

public class InvalidSprintIdException extends RuntimeException {
    public InvalidSprintIdException() {
        super("Sprint identifier must not be blank");
    }
}
