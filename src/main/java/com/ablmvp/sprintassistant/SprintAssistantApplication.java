package com.ablmvp.sprintassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Minimal application entry point for the Spring XD Sprint Health MVP backend.
 *
 * <p>The application starts the CSV-backed read-only API and loads the configured
 * historical dataset during application startup.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SprintAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintAssistantApplication.class, args);
    }
}
