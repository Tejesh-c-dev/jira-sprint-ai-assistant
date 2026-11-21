package com.ablmvp.sprintassistant.live.jira;

import com.ablmvp.sprintassistant.live.model.SprintIssue;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class JiraIssueMapper {
    public SprintIssue map(JsonNode issue, String storyPointsField) {
        JsonNode fields = issue.path("fields");
        JsonNode statusNode = fields.path("status");
        String status = text(statusNode, "name", "Unknown");
        String category = statusNode.path("statusCategory").path("key").asText("");
        JsonNode assigneeNode = fields.path("assignee");
        String assignee = assigneeNode.isMissingNode() || assigneeNode.isNull()
                ? "Unassigned" : text(assigneeNode, "displayName", "Unknown");
        JsonNode pointsNode = fields.path(storyPointsField);
        Double points = pointsNode.isNumber() ? pointsNode.asDouble() : null;
        LocalDate due = null;
        String dueText = fields.path("duedate").asText("");
        try { if (!dueText.isBlank()) due = LocalDate.parse(dueText); } catch (RuntimeException ignored) { }
        String blocker = status.toLowerCase().contains("block") ? "Issue status is " + status : null;
        if (blocker == null) {
            for (JsonNode link : fields.path("issuelinks")) {
                String inward = link.path("type").path("inward").asText("");
                if (inward.toLowerCase().contains("block") && link.has("inwardIssue")) {
                    blocker = "Blocked by " + link.path("inwardIssue").path("key").asText("another issue");
                    break;
                }
            }
        }
        return new SprintIssue(issue.path("key").asText(""), fields.path("summary").asText(""), status,
                category, assignee, points, fields.path("priority").path("name").asText("Unspecified"),
                due, blocker != null, blocker);
    }

    private String text(JsonNode node, String property, String fallback) {
        String value = node.path(property).asText("");
        return value.isBlank() ? fallback : value;
    }
}
