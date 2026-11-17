package com.ablmvp.sprintassistant.data.csv;

import com.ablmvp.sprintassistant.config.DataSourceProperties;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.IssueSprintLink;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CsvDataLoaderTest {
    @TempDir
    Path dataDirectory;

    @Test
    void loadsAllThreeFilesByHeaderAndPreservesCsvValuesAndOrder() throws Exception {
        write("issues.csv", String.join(",", ProvisionalCsvMapping.ISSUE_HEADERS) + "\n"
                + "XD-2,\"Summary, with comma\",Bug,Open,3.50,Major,\"Ada \"\"Lovelace\"\"\","
                + "\"Description line 1\nDescription line 2\",creator,reporter,,,,,,,,,,,,,,,\n"
                + "XD-1,Second,Task,,0,, ,,,,,,,,,,,,,,,\n");
        write("issue_sprint_summary.csv", String.join(",", ProvisionalCsvMapping.LINK_HEADERS) + "\n"
                + "S-1,XD-2,STORY-1,Completed,Ada,5,\"Link, summary\",1.25,2.50,PROJ\n"
                + "S-1,XD-2,STORY-2,Punted,,,,,,,\n");
        write("sprints.csv", String.join(",", ProvisionalCsvMapping.SPRINT_HEADERS) + "\n"
                + "S-1,\"Sprint, One\",CLOSED,2020-01-02,2020-01-15,2020-01-16,14,3,2,1,1,0,0,XD-2,"
                + "3.0,4.0,1.0,1.5,2.0,2.5,0,0\n");

        var result = new CsvDataLoader(properties()).load(DataKind.SYNTHETIC_FIXTURE);

        assertThat(result.usabilityState()).isEqualTo(UsabilityState.USABLE_WITH_WARNINGS);
        assertThat(result.issues()).extracting(Issue::issueKey).containsExactly("XD-2", "XD-1");
        assertThat(result.issues().get(0).summary()).isEqualTo("Summary, with comma");
        assertThat(result.issues().get(0).assignee()).isEqualTo("Ada \"Lovelace\"");
        assertThat(result.issues().get(0).description()).contains("line 1\nDescription line 2");
        assertThat(result.issues().get(0).storyPoint()).isEqualByComparingTo("3.50");
        assertThat(result.issues().get(1).storyPoint()).isEqualByComparingTo("0");
        assertThat(result.issues().get(1).assignee()).isNull();

        assertThat(result.issueSprintLinks()).hasSize(2);
        assertThat(result.issueSprintLinks()).extracting(IssueSprintLink::linkId)
                .containsExactly("ISSUE_SPRINT_SUMMARY-1", "ISSUE_SPRINT_SUMMARY-2");
        assertThat(result.issueSprintLinks()).extracting(IssueSprintLink::storyId)
                .containsExactly("STORY-1", "STORY-2");
        assertThat(result.issueSprintLinks().get(0).initialStoryPoint()).isEqualByComparingTo("1.25");
        assertThat(result.issueSprintLinks().get(1).initialStoryPoint()).isNull();

        Sprint sprint = result.sprints().get(0);
        assertThat(sprint.name()).isEqualTo("Sprint, One");
        assertThat(sprint.startDate()).hasToString("2020-01-02");
        assertThat(sprint.recordedIssueCount()).isEqualTo(2);
        assertThat(sprint.estimateSums().completedInitial()).isEqualByComparingTo("3.0");
        assertThat(result.provenance().dataKind()).isEqualTo(DataKind.SYNTHETIC_FIXTURE);
        assertThat(result.provenance().fileFingerprints()).extracting(f -> f.fileRole())
                .containsExactly(FileRole.ISSUES, FileRole.ISSUE_SPRINT_SUMMARY, FileRole.SPRINTS);
    }

    @Test
    void sourceRecordNumbersExcludeHeaderAndRepeatedPairsRemainSeparate() throws Exception {
        write("issues.csv", String.join(",", ProvisionalCsvMapping.ISSUE_HEADERS) + "\n"
                + issueRow("XD-1") + "\n" + issueRow("XD-2") + "\n");
        write("issue_sprint_summary.csv", String.join(",", ProvisionalCsvMapping.LINK_HEADERS) + "\n"
                + linkRow("S-1", "XD-1", "A") + "\n" + linkRow("S-1", "XD-1", "B") + "\n");
        write("sprints.csv", String.join(",", ProvisionalCsvMapping.SPRINT_HEADERS) + "\n"
                + "S-1,,,,,,,,,,,,,,,,,,,,,,\n");

        var result = new CsvDataLoader(properties()).load(DataKind.SYNTHETIC_FIXTURE);

        assertThat(result.issues().get(1).sourceRef().recordNumber()).isEqualTo(2);
        assertThat(result.issueSprintLinks()).extracting(link -> link.sourceRef().recordNumber())
                .containsExactly(1, 2);
        assertThat(result.issueSprintLinks()).extracting(IssueSprintLink::linkId)
                .doesNotHaveDuplicates();
        assertThat(result.sprints().get(0).sourceRef().fileRole()).isEqualTo(FileRole.SPRINTS);
    }

    private DataSourceProperties properties() {
        return new DataSourceProperties(dataDirectory.toString(), "issues.csv",
                "issue_sprint_summary.csv", "sprints.csv");
    }

    private void write(String file, String content) throws Exception {
        Files.writeString(dataDirectory.resolve(file), content);
    }

    private String issueRow(String key) {
        return key + ",summary,Task,Open,1,Major,Ada,,,,,,,,,,,,,,,";
    }

    private String linkRow(String sprint, String issue, String story) {
        return String.join(",", sprint, issue, story, "Completed", "Ada", "1", "summary", "1", "1", "PROJ");
    }
}
