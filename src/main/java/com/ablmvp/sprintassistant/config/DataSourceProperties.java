package com.ablmvp.sprintassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for the three CSV inputs used by the data layer.
 *
 * <p>The CSV files are deliberately not checked here. Their existence and contents are the
 * loader's responsibility. This class only validates configuration and resolves safe paths.</p>
 */
@ConfigurationProperties(prefix = "app.data")
public record DataSourceProperties(
        String directory,
        String issuesFilename,
        String issueSprintSummaryFilename,
        String sprintsFilename) {

    public DataSourceProperties {
        requireNonBlank("directory", directory);
        requireNonBlank("issuesFilename", issuesFilename);
        requireNonBlank("issueSprintSummaryFilename", issueSprintSummaryFilename);
        requireNonBlank("sprintsFilename", sprintsFilename);
        validateFilename("issuesFilename", issuesFilename);
        validateFilename("issueSprintSummaryFilename", issueSprintSummaryFilename);
        validateFilename("sprintsFilename", sprintsFilename);
    }

    public Path dataDirectory() {
        return Paths.get(directory).normalize();
    }

    public Path issuesPath() {
        return resolveFilename(issuesFilename);
    }

    public Path issueSprintSummaryPath() {
        return resolveFilename(issueSprintSummaryFilename);
    }

    public Path sprintsPath() {
        return resolveFilename(sprintsFilename);
    }

    private Path resolveFilename(String filename) {
        return dataDirectory().resolve(filename).normalize();
    }

    private static void requireNonBlank(String property, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must not be null or blank");
        }
    }

    private static void validateFilename(String property, String filename) {
        Path path = Paths.get(filename);
        if (path.isAbsolute()) {
            throw new IllegalArgumentException(property + " must be a relative filename");
        }
        for (Path segment : path) {
            if ("..".equals(segment.toString())) {
                throw new IllegalArgumentException(property + " must not contain '..'");
            }
        }
    }
}
