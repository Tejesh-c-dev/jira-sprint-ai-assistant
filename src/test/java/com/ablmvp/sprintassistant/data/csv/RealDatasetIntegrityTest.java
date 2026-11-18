package com.ablmvp.sprintassistant.data.csv;

import com.ablmvp.sprintassistant.config.DataSourceProperties;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RealDatasetIntegrityTest {
    private final Path realData = Path.of("data/spring-xd");

    @Test
    void suppliedRealDatasetLoadsWithoutBlockingFindings() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(realData));
        DataSourceProperties properties = new DataSourceProperties(
                realData.toString(), "issues.csv",
                "issue_sprint_summary.csv", "sprints.csv");

        LoadResult result = new CsvDataLoader(properties).load(DataKind.ORIGINAL_LOCAL);

        assertThat(result.sprints()).hasSize(66);
        assertThat(result.issueSprintLinks()).hasSize(2861);
        assertThat(result.issues()).hasSize(1902);
        assertThat(result.findings()).noneMatch(finding -> finding.blocking());
        assertThat(result.usabilityState()).isEqualTo(UsabilityState.USABLE_WITH_WARNINGS);
    }

    @Test
    void realDataDirectoryIsPresentOnlyAsLocalIgnoredInput() {
        assertThatNoException().isThrownBy(() -> Files.isDirectory(realData));
    }

    @Test
    void negativeEstimateRemainsAsSourceEvidence() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(realData));
        DataSourceProperties properties = new DataSourceProperties(
                realData.toString(), "issues.csv",
                "issue_sprint_summary.csv", "sprints.csv");
        LoadResult result = new CsvDataLoader(properties).load(DataKind.ORIGINAL_LOCAL);
        assertThat(result.issueSprintLinks())
                .anyMatch(link -> link.initialStoryPoint() != null
                        && link.initialStoryPoint().signum() < 0);
    }
}
