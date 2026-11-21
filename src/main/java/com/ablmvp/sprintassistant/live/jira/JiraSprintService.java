package com.ablmvp.sprintassistant.live.jira;

import com.ablmvp.sprintassistant.config.AppProperties;
import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import com.ablmvp.sprintassistant.live.model.SprintInfo;
import com.ablmvp.sprintassistant.live.model.SprintIssue;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class JiraSprintService {
    private final JiraClient client;
    private final JiraIssueMapper mapper;
    private final AppProperties properties;

    public JiraSprintService(JiraClient client, JiraIssueMapper mapper, AppProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.properties = properties;
    }

    public LiveSprintSnapshot fetchActiveSprint() {
        String boardId = properties.getJira().getBoardId();
        if (boardId == null || boardId.isBlank()) throw new IllegalStateException("Jira board is not configured. Set JIRA_BOARD_ID.");
        JsonNode values = client.get("/rest/agile/1.0/board/" + boardId + "/sprint?state=active").path("values");
        if (values.isEmpty()) return new LiveSprintSnapshot(null, List.of(), Instant.now(), "CONNECTED", null);
        JsonNode active = values.get(0);
        SprintInfo sprint = new SprintInfo(active.path("id").asText(), active.path("name").asText("Active sprint"),
                instant(active.path("startDate").asText(null)), instant(active.path("endDate").asText(null)), active.path("state").asText("active"));
        List<SprintIssue> issues = new ArrayList<>();
        int startAt = 0;
        String storyField = properties.getJira().getStoryPointsField();
        while (true) {
            JsonNode page = client.get("/rest/agile/1.0/sprint/" + sprint.id() + "/issue?startAt=" + startAt
                    + "&maxResults=100&fields=summary,status,assignee,priority,duedate,issuelinks," + storyField);
            JsonNode pageIssues = page.path("issues");
            pageIssues.forEach(issue -> issues.add(mapper.map(issue, storyField)));
            startAt += pageIssues.size();
            if (pageIssues.isEmpty() || startAt >= page.path("total").asInt(startAt)) break;
        }
        return new LiveSprintSnapshot(sprint, issues, Instant.now(), "CONNECTED", null);
    }

    private Instant instant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (RuntimeException ignored) { }
        try { return OffsetDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toInstant(); } catch (RuntimeException ignored) { }
        try { return OffsetDateTime.parse(value).toInstant(); } catch (RuntimeException ignored) { return null; }
    }
}
