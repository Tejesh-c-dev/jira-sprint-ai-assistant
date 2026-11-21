package com.ablmvp.sprintassistant.live.model;

import java.time.Instant;

public record SprintInfo(String id, String name, Instant startDate, Instant endDate, String state) { }
