package com.ablmvp.sprintassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String mode = "csv";
    private final Jira jira = new Jira();
    private final Ai ai = new Ai();
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Jira getJira() { return jira; }
    public Ai getAi() { return ai; }
    public static class Jira {
        private String baseUrl = "", email = "", apiToken = "", boardId = "", storyPointsField = "customfield_10016";
        private long pollIntervalMs = 60000;
        public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String value) { baseUrl = value; }
        public String getEmail() { return email; } public void setEmail(String value) { email = value; }
        public String getApiToken() { return apiToken; } public void setApiToken(String value) { apiToken = value; }
        public String getBoardId() { return boardId; } public void setBoardId(String value) { boardId = value; }
        public String getStoryPointsField() { return storyPointsField; } public void setStoryPointsField(String value) { storyPointsField = value; }
        public long getPollIntervalMs() { return pollIntervalMs; } public void setPollIntervalMs(long value) { pollIntervalMs = value; }
    }
    public static class Ai {
        private String openrouterBaseUrl = "https://openrouter.ai/api/v1", openrouterApiKey = "", openrouterModel = "";
        public String getOpenrouterBaseUrl() { return openrouterBaseUrl; } public void setOpenrouterBaseUrl(String value) { openrouterBaseUrl = value; }
        public String getOpenrouterApiKey() { return openrouterApiKey; } public void setOpenrouterApiKey(String value) { openrouterApiKey = value; }
        public String getOpenrouterModel() { return openrouterModel; } public void setOpenrouterModel(String value) { openrouterModel = value; }
    }
}
