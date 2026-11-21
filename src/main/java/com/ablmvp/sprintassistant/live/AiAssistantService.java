package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.config.AppProperties;
import com.ablmvp.sprintassistant.live.model.AskResponse;
import com.ablmvp.sprintassistant.live.model.EmployeeWorkSignal;
import com.ablmvp.sprintassistant.live.model.LiveMetrics;
import com.ablmvp.sprintassistant.live.model.LiveSprintSnapshot;
import com.ablmvp.sprintassistant.live.model.SprintIssue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private static final String SYSTEM_PROMPT = "You are a read-only Jira sprint assistant. Use only the supplied Jira snapshot. Explain conclusions using snapshot evidence and never invent missing metrics. Avoid claims about employee performance; use terms such as needs attention, blocked, or workload risk. Never call a person lazy, weak, or underperforming. Never modify Jira. Recommend human review for sensitive conclusions. Return only a JSON object with answer (string), evidence (string array), and recommendations (string array).";
    private final AppProperties properties;
    private final LiveSprintCache cache;
    private final LiveMetricsService metricsService;
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public AiAssistantService(AppProperties properties, LiveSprintCache cache, LiveMetricsService metricsService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.cache = cache;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        var requests = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requests.setReadTimeout(Duration.ofSeconds(35));
        this.client = RestClient.builder().requestFactory(requests).build();
    }

    public AskResponse ask(String question) {
        LiveSprintSnapshot snapshot = cache.get();
        var ai = properties.getAi();
        if (ai.getOpenrouterApiKey().isBlank() || ai.getOpenrouterModel().isBlank())
            return fallback(question, snapshot, "OpenRouter is not configured. Set OPENROUTER_API_KEY and OPENROUTER_MODEL.");
        try {
            Map<String, Object> request = Map.of("model", ai.getOpenrouterModel(), "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"), "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", "Question: " + question + "\n\nSupplied Jira snapshot (JSON):\n" + snapshotJson(snapshot))));
            JsonNode response = client.post().uri(ai.getOpenrouterBaseUrl().replaceAll("/+$", "") + "/chat/completions")
                    .header("Authorization", "Bearer " + ai.getOpenrouterApiKey()).body(request).retrieve().body(JsonNode.class);
            JsonNode output = objectMapper.readTree(response.path("choices").path(0).path("message").path("content").asText(""));
            return new AskResponse(output.path("answer").asText("The model returned no answer."), strings(output.path("evidence")),
                    strings(output.path("recommendations")), Instant.now(), true, null);
        } catch (Exception error) {
            log.warn("OpenRouter request failed: {}", error.getMessage());
            return fallback(question, snapshot, "OpenRouter is unavailable; showing a rule-based answer.");
        }
    }

    private AskResponse fallback(String question, LiveSprintSnapshot snapshot, String warning) {
        LiveMetrics metrics = metricsService.metrics(snapshot);
        List<String> evidence = new ArrayList<>(metrics.evidence());
        if (snapshot.dataSourceStatus().equals("UNAVAILABLE")) evidence.add("Jira is unavailable; figures use the last successful synchronization when present");
        List<String> recommendations = new ArrayList<>();
        if (metrics.blockedIssues() > 0) recommendations.add("Review blocked issues and confirm their dependencies");
        if (metrics.overdueIssues() > 0) recommendations.add("Confirm ownership and next steps for overdue issues");
        if (metrics.remainingIssues() > 0) recommendations.add("Review remaining work against the sprint end date");
        List<EmployeeWorkSignal> signals = metricsService.team(snapshot);
        signals.stream().filter(s -> !s.status().equals("OK"))
                .forEach(s -> recommendations.add("Check in with " + s.assignee() + " about: " + String.join(", ", s.reasons())));
        if (recommendations.isEmpty()) recommendations.add("Continue monitoring the sprint and review the next synchronization");
        String lower = question.toLowerCase();
        String answer;
        if (lower.contains("who") || lower.contains("support")) {
            List<String> needingSupport = signals.stream().filter(s -> !s.status().equals("OK"))
                    .map(s -> s.assignee() + " (" + s.status().toLowerCase().replace('_', ' ') + ")").toList();
            answer = needingSupport.isEmpty() ? "No team support signals are present in the current snapshot."
                    : "Current support signals: " + String.join("; ", needingSupport) + ". Review the evidence with each person.";
        } else if (lower.contains("workload") || lower.contains("balance")) {
            answer = signals.isEmpty() ? "There is not enough assigned work data to assess workload distribution."
                    : "Workload signals use remaining issue counts relative to the team average and in-progress items. Review the team reasons with the team before reallocating work.";
        } else if (lower.contains("priorit")) {
            answer = metrics.blockedIssues() + metrics.overdueIssues() > 0
                    ? "Review blocked and overdue issues first, then confirm the remaining work fits the sprint."
                    : "Review remaining work against the sprint end date and confirm priority with the team.";
        } else if (lower.contains("summar")) {
            answer = "The current sprint has " + metrics.totalIssues() + " issues: " + metrics.completedIssues()
                    + " completed and " + metrics.remainingIssues() + " remaining. Sprint health is " + metrics.health().toLowerCase() + ".";
        } else {
            answer = "Sprint health is " + metrics.health().toLowerCase().replace('_', ' ') + ". "
                    + (metrics.evidence().isEmpty() ? "No risk indicators are present in the current snapshot." : String.join("; ", metrics.evidence()) + ".");
        }
        if (snapshot.issues().isEmpty()) evidence.add("No sprint issue data is available in the current snapshot");
        return new AskResponse(answer, evidence, recommendations, Instant.now(), false, warning);
    }

    private String snapshotJson(LiveSprintSnapshot snapshot) throws Exception {
        List<Map<String, Object>> issues = snapshot.issues().stream().map(this::issueMap).toList();
        return objectMapper.writeValueAsString(Map.of("metrics", metricsService.metrics(snapshot),
                "team", metricsService.team(snapshot), "issues", issues,
                "lastSyncedAt", snapshot.lastSyncedAt() == null ? "unavailable" : snapshot.lastSyncedAt().toString()));
    }

    private Map<String, Object> issueMap(SprintIssue issue) {
        return Map.of("key", issue.key(), "summary", issue.summary(), "status", issue.status(), "assignee", issue.assignee(),
                "storyPoints", issue.storyPoints() == null ? 0 : issue.storyPoints(), "priority", issue.priority(),
                "dueDate", issue.dueDate() == null ? "unknown" : issue.dueDate().toString(),
                "overdue", issue.overdue(LocalDate.now()), "blocked", issue.blocked(),
                "blockerReason", issue.blockerReason() == null ? "none" : issue.blockerReason());
    }

    private List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) node.forEach(value -> values.add(value.asText()));
        return values;
    }
}
