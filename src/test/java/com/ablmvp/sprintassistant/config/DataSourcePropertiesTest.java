package com.ablmvp.sprintassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataSourcePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "app.data.directory=data",
                    "app.data.issues-filename=issues.csv",
                    "app.data.issue-sprint-summary-filename=issue_sprint_summary.csv",
                    "app.data.sprints-filename=sprints.csv");

    @Test
    void usesDocumentedDefaultFilenames() {
        contextRunner.run(context -> {
            DataSourceProperties properties = context.getBean(DataSourceProperties.class);

            assertThat(properties.directory()).isEqualTo("data");
            assertThat(properties.issuesFilename()).isEqualTo("issues.csv");
            assertThat(properties.issueSprintSummaryFilename()).isEqualTo("issue_sprint_summary.csv");
            assertThat(properties.sprintsFilename()).isEqualTo("sprints.csv");
            assertThat(properties.issuesPath()).isEqualTo(Path.of("data", "issues.csv"));
        });
    }

    @Test
    void resolvesCustomDirectoryAndFilenames() {
        contextRunner
                .withPropertyValues(
                        "app.data.directory=build/test-data",
                        "app.data.issues-filename=inputs/issues.csv",
                        "app.data.issue-sprint-summary-filename=inputs/summary.csv",
                        "app.data.sprints-filename=metadata/sprints.csv")
                .run(context -> {
                    DataSourceProperties properties = context.getBean(DataSourceProperties.class);

                    assertThat(properties.issuesPath()).isEqualTo(Path.of("build/test-data", "inputs", "issues.csv"));
                    assertThat(properties.issueSprintSummaryPath())
                            .isEqualTo(Path.of("build/test-data", "inputs", "summary.csv"));
                    assertThat(properties.sprintsPath())
                            .isEqualTo(Path.of("build/test-data", "metadata", "sprints.csv"));
                });
    }

    @Test
    void supportsEnvironmentVariableNames() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("test-environment", Map.of(
                                "APP_DATA_DIRECTORY", "environment-data",
                                "APP_DATA_ISSUES_FILENAME", "env-issues.csv",
                                "APP_DATA_ISSUE_SPRINT_SUMMARY_FILENAME", "env-summary.csv",
                                "APP_DATA_SPRINTS_FILENAME", "env-sprints.csv"))))
                .run(context -> {
                    DataSourceProperties properties = context.getBean(DataSourceProperties.class);

                    assertThat(properties.directory()).isEqualTo("environment-data");
                    assertThat(properties.issuesFilename()).isEqualTo("env-issues.csv");
                    assertThat(properties.issueSprintSummaryFilename()).isEqualTo("env-summary.csv");
                    assertThat(properties.sprintsFilename()).isEqualTo("env-sprints.csv");
                });
    }

    @Test
    void rejectsBlankFilename() {
        assertThatThrownBy(() -> new DataSourceProperties("data", " ", "summary.csv", "sprints.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuesFilename");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> new DataSourceProperties("data", "../issues.csv", "summary.csv", "sprints.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuesFilename");
    }

    @Test
    void rejectsAbsoluteFilename() {
        assertThatThrownBy(() -> new DataSourceProperties("data", Path.of("outside.csv").toAbsolutePath().toString(),
                "summary.csv", "sprints.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relative filename");
    }

    @Test
    void initializesWhenCsvFilesAreMissing() {
        contextRunner
                .withPropertyValues("app.data.directory=directory-without-csv-files")
                .run(context -> {
                    DataSourceProperties properties = context.getBean(DataSourceProperties.class);

                    assertThat(properties.issuesPath()).doesNotExist();
                    assertThat(properties.issueSprintSummaryPath()).doesNotExist();
                    assertThat(properties.sprintsPath()).doesNotExist();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class PropertiesConfiguration {
    }
}
