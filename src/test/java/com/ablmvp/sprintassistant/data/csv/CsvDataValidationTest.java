package com.ablmvp.sprintassistant.data.csv;

import com.ablmvp.sprintassistant.config.DataSourceProperties;
import com.ablmvp.sprintassistant.domain.DataKind;
import com.ablmvp.sprintassistant.domain.UsabilityState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CsvDataValidationTest {
    @TempDir Path dir;

    @Test void missingFilesAreBlocking() throws Exception { var r=load(); assertThat(r.usabilityState()).isEqualTo(UsabilityState.UNUSABLE); assertThat(r.findings()).extracting("errorCode").contains("FILE_MISSING"); }
    @Test void missingHeaderIsBlocking() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), "summary\ntext\n"); var r=load(); assertThat(r.findings()).extracting("errorCode").contains("HEADER_MISSING"); assertThat(r.usabilityState()).isEqualTo(UsabilityState.UNUSABLE); }
    @Test void duplicateHeaderIsReported() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), "key,key\na,b\n"); var r=load(); assertThat(r.findings()).extracting("errorCode").contains("HEADER_DUPLICATE"); }
    @Test void emptyAndHeaderOnlyFilesAreBlocking() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), ""); assertThat(load().findings()).extracting("errorCode").contains("FILE_EMPTY"); base(); Files.writeString(dir.resolve("issues.csv"), "key\n"); assertThat(load().findings()).extracting("errorCode").contains("FILE_EMPTY"); }
    @Test void blankIdentifiersHaveSourceReferencesAndAreQuarantined() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), ProvisionalCsvMapping.ISSUE_HEADERS.get(0)+"\n \n"); var r=load(); assertThat(r.findings()).anyMatch(f->f.errorCode().equals("REQUIRED_IDENTIFIER_BLANK")&&f.recordNumber()==1&&f.fileRole().name().equals("ISSUES")); assertThat(r.quarantinedRecords()).anyMatch(q -> q.sourceRef().fileRole().name().equals("ISSUES") && q.sourceRef().recordNumber() == 1); }
    @Test void invalidDateAndNumberAreWarningsAndRemainNull() throws Exception { base(); Files.writeString(dir.resolve("sprints.csv"), String.join(",",ProvisionalCsvMapping.SPRINT_HEADERS)+"\n"+sprint("S-1","2020-99-99","oops")); var r=load(); assertThat(r.findings()).extracting("errorCode").contains("DATE_INVALID","NUMBER_INVALID"); assertThat(r.sprints().get(0).startDate()).isNull(); }
    @Test void negativeNumbersAreRejectedAndRemainNull() throws Exception { base(); Files.writeString(dir.resolve("sprints.csv"), String.join(",",ProvisionalCsvMapping.SPRINT_HEADERS)+"\n"+sprint("S-1","2020-01-01","-1")); var r=load(); assertThat(r.findings()).extracting("errorCode").contains("NUMBER_NEGATIVE"); assertThat(r.sprints().get(0).lengthDays()).isNull(); }
    @Test void exactAndConflictingIssueDuplicatesKeepFirstAndQuarantineRest() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), String.join(",",ProvisionalCsvMapping.ISSUE_HEADERS)+"\n"+issue("I-1","same")+issue("I-1","same")+issue("I-1","different")); var r=load(); assertThat(r.issues()).hasSize(1); assertThat(r.quarantinedRecords()).hasSize(2); assertThat(r.findings()).extracting("errorCode").contains("DUPLICATE_MASTER_IDENTICAL","DUPLICATE_MASTER_CONFLICT"); }
    @Test void exactAndConflictingSprintDuplicatesKeepFirstAndQuarantineRest() throws Exception { base(); Files.writeString(dir.resolve("sprints.csv"), String.join(",",ProvisionalCsvMapping.SPRINT_HEADERS)+"\n"+sprint("S-1","2020-01-01","1")+sprint("S-1","2020-01-01","1")+sprint("S-1","2020-02-01","1")); var r=load(); assertThat(r.sprints()).hasSize(1); assertThat(r.quarantinedRecords()).hasSize(2); }
    @Test void unknownSprintIsQuarantinedButUnknownIssueLinkIsRetained() throws Exception { base(); Files.writeString(dir.resolve("issue_sprint_summary.csv"), String.join(",",ProvisionalCsvMapping.LINK_HEADERS)+"\n"+link("S-1","UNKNOWN")+link("MISSING","I-1")); var r=load(); assertThat(r.issueSprintLinks()).hasSize(1); assertThat(r.quarantinedRecords()).hasSize(1); assertThat(r.findings()).extracting("errorCode").contains("UNKNOWN_SPRINT_REFERENCE","UNKNOWN_ISSUE_REFERENCE"); }
    @Test void duplicateLinkPairsRemainDistinct() throws Exception { base(); Files.writeString(dir.resolve("issue_sprint_summary.csv"), String.join(",",ProvisionalCsvMapping.LINK_HEADERS)+"\n"+link("S-1","I-1")+link("S-1","I-1")); var r=load(); assertThat(r.issueSprintLinks()).hasSize(2); assertThat(r.findings()).extracting("errorCode").contains("DUPLICATE_LINK_PAIR"); }
    @Test void sentinelIsVisibleButNotCoercedAndMessagesDoNotContainRows() throws Exception { base(); Files.writeString(dir.resolve("issues.csv"), String.join(",",ProvisionalCsvMapping.ISSUE_HEADERS)+"\n"+issue("I-1","NA")); var r=load(); assertThat(r.issues().get(0).summary()).isEqualTo("NA"); assertThat(r.findings()).anyMatch(f->f.errorCode().equals("SENTINEL_UNKNOWN")); assertThat(r.findings()).allMatch(f->!f.reason().contains("I-1,NA")); }
    @Test void usabilityTransitionsCleanWarningAndBlocking() throws Exception { base(); assertThat(load().usabilityState()).isEqualTo(UsabilityState.USABLE); Files.writeString(dir.resolve("issue_sprint_summary.csv"), String.join(",",ProvisionalCsvMapping.LINK_HEADERS)+"\n"+link("S-1","UNKNOWN")); assertThat(load().usabilityState()).isEqualTo(UsabilityState.USABLE_WITH_WARNINGS); Files.delete(dir.resolve("sprints.csv")); assertThat(load().usabilityState()).isEqualTo(UsabilityState.UNUSABLE); }

    private com.ablmvp.sprintassistant.data.result.LoadResult load() throws Exception { return new CsvDataLoader(new DataSourceProperties(dir.toString(),"issues.csv","issue_sprint_summary.csv","sprints.csv")).load(DataKind.SYNTHETIC_FIXTURE); }
    private void base() throws Exception { Files.writeString(dir.resolve("issues.csv"),String.join(",",ProvisionalCsvMapping.ISSUE_HEADERS)+"\n"+issue("I-1","summary")); Files.writeString(dir.resolve("issue_sprint_summary.csv"),String.join(",",ProvisionalCsvMapping.LINK_HEADERS)+"\n"+link("S-1","I-1")); Files.writeString(dir.resolve("sprints.csv"),String.join(",",ProvisionalCsvMapping.SPRINT_HEADERS)+"\n"+sprint("S-1","2020-01-01","1")); }
    private static String issue(String id,String summary){return id+","+summary+","+String.join(",",Collections.nCopies(19,""))+"\n";}
    private static String link(String sprint,String issue){return String.join(",",sprint,issue,"STORY","Completed","Ada","1","summary","1","1","PROJ")+"\n";}
    private static String sprint(String id,String date,String length){return String.join(",",id,"Sprint", "CLOSED",date,"2020-01-15","2020-01-16",length,"1","1","1","0","0","0","", "1","1","0","0","0","0","0","0")+"\n";}
}
