package com.ablmvp.sprintassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Minimal application entry point for the Spring XD Sprint Health MVP backend.
 *
 * <p>This shell intentionally contains no web, data-loading or analysis code yet; the data
 * layer (Teammate 1) and the REST layer (Teammate 2) are added in later prompts.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SprintAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintAssistantApplication.class, args);
    }
}
