package com.ablmvp.sprintassistant.live.model;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(@NotBlank String question) { }
