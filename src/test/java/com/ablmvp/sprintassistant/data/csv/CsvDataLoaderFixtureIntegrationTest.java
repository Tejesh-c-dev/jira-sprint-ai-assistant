package com.ablmvp.sprintassistant.data.csv;

import com.ablmvp.sprintassistant.config.DataSourceProperties;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end coverage for the committed DATA-11 synthetic CSV set. */
class CsvDataLoaderFixtureIntegrationTest {
    @Test
    void loadsSyntheticFixtureSetWithRecordsProvenanceFindingsQuarantineAndUsability() throws Exception {
        Path fixtureDirectory = fixtureDirectory();
        DataSourceProperties properties = new DataSourceProperties(fixtureDirectory.toString(),
                "issues.csv", "issue_sprint_summary.csv", "sprints.csv");

        var result = new CsvDataLoader(properties).load(DataKind.SYNTHETIC_FIXTURE);

        assertThat(result.issues()).hasSize(3);
        assertThat(result.issues().get(0).issueKey()).isEqualTo("SYN-101");
        assertThat(result.issues().get(0).summary()).isEqualTo("Comma, capable summary");
        assertThat(result.issues().get(0).description()).isEqualTo(
                "First line of synthetic detail\nSecond line, still synthetic.");
        assertThat(result.issues().get(2).summary()).isNull();
        assertThat(result.issues()).allMatch(issue -> issue.sourceRef().fileRole() == FileRole.ISSUES);

        assertThat(result.sprints()).hasSize(2);
        assertThat(result.sprints().get(0).name()).isEqualTo("Synthetic Sprint, Alpha");
        assertThat(result.sprints().get(1).startDate()).isNull();
        assertThat(result.sprints()).allMatch(sprint -> sprint.sourceRef().fileRole() == FileRole.SPRINTS);

        assertThat(result.issueSprintLinks()).hasSize(4);
        assertThat(result.issueSprintLinks()).extracting(link -> link.sprintId())
                .containsExactly("SYN-SPRINT-1", "SYN-SPRINT-2", "SYN-SPRINT-1", "SYN-SPRINT-1");
        assertThat(result.issueSprintLinks()).extracting(link -> link.issueKey())
                .containsExactly("SYN-101", "SYN-101", "SYN-101", "SYN-999");
        assertThat(result.issueSprintLinks()).extracting(link -> link.sourceRef().recordNumber())
                .containsExactly(1, 2, 3, 4);
        assertThat(result.issueSprintLinks()).extracting(link -> link.linkId()).doesNotHaveDuplicates();

        assertThat(result.provenance().dataKind()).isEqualTo(DataKind.SYNTHETIC_FIXTURE);
        assertThat(result.provenance().fileFingerprints()).hasSize(3)
                .extracting(fingerprint -> fingerprint.fileRole())
                .containsExactly(FileRole.ISSUES, FileRole.ISSUE_SPRINT_SUMMARY, FileRole.SPRINTS);
        assertThat(result.provenance().licenceStatus()).contains("DATA-02");

        assertThat(result.findings()).extracting(finding -> finding.errorCode())
                .contains("UNKNOWN_SPRINT_REFERENCE", "UNKNOWN_ISSUE_REFERENCE", "DUPLICATE_LINK_PAIR");
        assertThat(result.findings()).allMatch(finding -> finding.recordNumber() == null
                || finding.recordNumber() > 0);
        assertThat(result.quarantinedRecords()).hasSize(1);
        assertThat(result.quarantinedRecords().get(0).sourceRef().fileRole())
                .isEqualTo(FileRole.ISSUE_SPRINT_SUMMARY);
        assertThat(result.quarantinedRecords().get(0).sourceRef().recordNumber()).isEqualTo(5);
        assertThat(result.quarantinedRecords().get(0).linkId()).isEqualTo("ISSUE_SPRINT_SUMMARY-5");
        assertThat(result.usabilityState()).isEqualTo(UsabilityState.USABLE_WITH_WARNINGS);
    }

    private Path fixtureDirectory() throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/data-11").toURI());
    }
}
