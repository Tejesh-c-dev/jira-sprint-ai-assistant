package com.ablmvp.sprintassistant.live.jira;

import com.ablmvp.sprintassistant.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class JiraClient {
    private final AppProperties properties;
    private final RestClient client;

    public JiraClient(AppProperties properties) {
        this.properties = properties;
        var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(25));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    public JsonNode get(String path) {
        var jira = properties.getJira();
        if (blank(jira.getBaseUrl()) || blank(jira.getEmail()) || blank(jira.getApiToken())) {
            throw new IllegalStateException("Jira is not configured. Set JIRA_BASE_URL, JIRA_EMAIL, and JIRA_API_TOKEN.");
        }
        String auth = Base64.getEncoder().encodeToString((jira.getEmail() + ":" + jira.getApiToken()).getBytes(StandardCharsets.UTF_8));
        return client.get().uri(jira.getBaseUrl().replaceAll("/+$", "") + path)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve().body(JsonNode.class);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
