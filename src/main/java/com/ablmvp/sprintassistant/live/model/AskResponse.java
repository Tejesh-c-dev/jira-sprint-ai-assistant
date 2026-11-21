package com.ablmvp.sprintassistant.live.model;

import java.time.Instant;
import java.util.List;

public record AskResponse(String answer, List<String> evidence, List<String> recommendations,
                          Instant generatedAt, boolean aiAvailable, String warning) { }
