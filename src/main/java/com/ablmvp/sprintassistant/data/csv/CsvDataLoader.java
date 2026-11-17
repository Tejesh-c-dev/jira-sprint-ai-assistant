package com.ablmvp.sprintassistant.data.csv;

import com.ablmvp.sprintassistant.config.DataSourceProperties;
import com.ablmvp.sprintassistant.data.result.*;
import com.ablmvp.sprintassistant.domain.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Loads the three configured CSV files and applies the DATA-10 validation policy. */
public final class CsvDataLoader {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DataSourceProperties properties;

    public CsvDataLoader(DataSourceProperties properties) { this.properties = Objects.requireNonNull(properties); }
    public LoadResult load() throws IOException { return load(DataKind.ORIGINAL_LOCAL); }

    public LoadResult load(DataKind dataKind) throws IOException {
        Objects.requireNonNull(dataKind, "dataKind must not be null");
        List<ValidationFinding> findings = new ArrayList<>();
        List<QuarantinedRecord> quarantine = new ArrayList<>();
        List<Issue> issues = resolveIssues(parseIssues(properties.issuesPath(), findings, quarantine), findings, quarantine);
        List<Sprint> sprints = resolveSprints(parseSprints(properties.sprintsPath(), findings, quarantine), findings, quarantine);
        List<IssueSprintLink> parsedLinks = parseLinks(properties.issueSprintSummaryPath(), findings, quarantine);
        Set<String> sprintIds = ids(sprints, Sprint::sprintId);
        Set<String> issueKeys = ids(issues, Issue::issueKey);
        Map<String, List<SourceRef>> pairs = new LinkedHashMap<>();
        List<IssueSprintLink> links = new ArrayList<>();
        for (IssueSprintLink link : parsedLinks) {
            if (link.sprintId() == null || link.issueKey() == null) {
                quarantine.add(new QuarantinedRecord(link.sourceRef(), "required link identifier is blank"));
                continue;
            }
            if (!sprintIds.contains(link.sprintId())) {
                findings.add(finding(link.sourceRef(), "sprintId", FindingSeverity.ERROR,
                        ValidationErrorCode.UNKNOWN_SPRINT_REFERENCE, "link references an unknown sprint", link.sprintId(), false));
                quarantine.add(new QuarantinedRecord(link.sourceRef(), "unknown sprint reference", link.linkId()));
                continue;
            }
            if (!issueKeys.contains(link.issueKey())) findings.add(finding(link.sourceRef(), "issueKey", FindingSeverity.WARNING,
                    ValidationErrorCode.UNKNOWN_ISSUE_REFERENCE, "link references an unknown issue; link retained", link.issueKey(), false));
            String pair = link.sprintId() + "\u0000" + link.issueKey();
            pairs.computeIfAbsent(pair, ignored -> new ArrayList<>()).add(link.sourceRef());
            links.add(link);
        }
        for (List<SourceRef> refs : pairs.values()) if (refs.size() > 1)
            findings.add(finding(refs.get(0), null, FindingSeverity.WARNING, ValidationErrorCode.DUPLICATE_LINK_PAIR,
                    "duplicate sprint/issue link pair retained; link rows are not deduplicated", null, false));

        DatasetProvenance provenance = new DatasetProvenance("Jira Sprint Assistant dataset", dataKind,
                "Ayushi1429/Agile-Sprint-Analysis; RandulaKoralage/AgileScrumSprintVelocityDataSet (2020 dataset)",
                "https://github.com/Ayushi1429/Agile-Sprint-Analysis",
                "DATA-02 blocked: no reuse or redistribution grant was identified",
                "prepared in 2020; extraction date not recorded",
                List.of("filtered historical extract", "developer count is derived", "dependency columns may be empty"),
                Instant.now(), fingerprints());
        boolean blocking = findings.stream().anyMatch(ValidationFinding::blocking);
        UsabilityState state = blocking ? UsabilityState.UNUSABLE : findings.isEmpty() ? UsabilityState.USABLE : UsabilityState.USABLE_WITH_WARNINGS;
        return new LoadResult(issues, sprints, links, quarantine, findings, provenance, state);
    }

    private static <T> Set<String> ids(List<T> values, java.util.function.Function<T, String> f) {
        Set<String> result = new HashSet<>(); for (T value : values) { String id = f.apply(value); if (id != null) result.add(id); } return result;
    }

    public List<Issue> parseIssues(Path path, List<ValidationFinding> findings) throws IOException {
        return parseIssues(path, findings, new ArrayList<>());
    }
    private List<Issue> parseIssues(Path path, List<ValidationFinding> findings, List<QuarantinedRecord> quarantine) throws IOException {
        return parse(path, FileRole.ISSUES, List.of("key"), findings, (r, s) -> new Issue(
                text(r,"key"),text(r,"summary"),text(r,"issueType"),text(r,"status"),decimal(r,"storyPoint",s,findings),
                text(r,"priority"),text(r,"assignee"),text(r,"description"),text(r,"creator"),text(r,"reporter"),
                text(r,"watchCount"),text(r,"commentCount"),text(r,"votes"),text(r,"fixVersions"),text(r,"affectedVersions"),
                text(r,"issueLinks"),text(r,"blockedBy"),text(r,"blocks"),text(r,"dependedOnBy"),text(r,"dependedOn"),text(r,"subtasks"),s), quarantine);
    }
    public List<IssueSprintLink> parseIssueSprintSummary(Path path, List<ValidationFinding> findings) throws IOException { return parseLinks(path, findings); }
    private List<IssueSprintLink> parseLinks(Path path, List<ValidationFinding> findings) throws IOException {
        return parseLinks(path, findings, new ArrayList<>());
    }
    private List<IssueSprintLink> parseLinks(Path path, List<ValidationFinding> findings, List<QuarantinedRecord> quarantine) throws IOException {
        return parse(path, FileRole.ISSUE_SPRINT_SUMMARY, List.of("sprintId","issueKey"), findings, (r,s) -> new IssueSprintLink(
                s.fileRole()+"-"+s.recordNumber(),text(r,"sprintId"),text(r,"issueKey"),text(r,"storyId"),text(r,"status"),
                text(r,"assignee"),text(r,"priorityId"),text(r,"summary"),decimal(r,"initialStoryPoint",s,findings),
                decimal(r,"currentStoryPoint",s,findings),text(r,"projectID"),s), quarantine);
    }
    public List<Sprint> parseSprints(Path path, List<ValidationFinding> findings) throws IOException {
        return parseSprints(path, findings, new ArrayList<>());
    }
    private List<Sprint> parseSprints(Path path, List<ValidationFinding> findings, List<QuarantinedRecord> quarantine) throws IOException {
        return parse(path, FileRole.SPRINTS, List.of("sprintId"), findings, (r,s) -> new Sprint(
                text(r,"sprintId"),text(r,"sprintName"),text(r,"sprintState"),date(r,"sprintStartDate",s,findings),
                date(r,"sprintEndDate",s,findings),date(r,"sprintCompleteDate",s,findings),integer(r,"SprintLength",s,findings),
                integer(r,"NoOfDevelopers",s,findings),integer(r,"totalNumberOfIssues",s,findings),integer(r,"completedIssuesCount",s,findings),
                integer(r,"issuesNotCompletedInCurrentSprint",s,findings),integer(r,"puntedIssues",s,findings),integer(r,"issuesCompletedInAnotherSprint",s,findings),
                listValue(r,"issueKeysAddedDuringSprint"),new SprintEstimateSums(
                decimal(r,"completedIssuesInitialEstimateSum",s,findings),decimal(r,"completedIssuesEstimateSum",s,findings),
                decimal(r,"puntedIssuesInitialEstimateSum",s,findings),decimal(r,"puntedIssuesEstimateSum",s,findings),
                decimal(r,"issuesNotCompletedInitialEstimateSum",s,findings),decimal(r,"issuesNotCompletedEstimateSum",s,findings),
                decimal(r,"issuesCompletedInAnotherSprintInitialEstimateSum",s,findings),decimal(r,"issuesCompletedInAnotherSprintEstimateSum",s,findings)),s), quarantine);
    }

    private <T> List<T> parse(Path path, FileRole role, List<String> required, List<ValidationFinding> findings, RecordMapper<T> mapper,
                              List<QuarantinedRecord> quarantine) throws IOException {
        if (!Files.exists(path)) { findings.add(fileFinding(role,ValidationErrorCode.FILE_MISSING,"required CSV file does not exist",path.toString(),true)); return List.of(); }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) { findings.add(fileFinding(role,ValidationErrorCode.FILE_UNREADABLE,"required CSV file is not readable",null,true)); return List.of(); }
        if (Files.size(path) == 0) { findings.add(fileFinding(role,ValidationErrorCode.FILE_EMPTY,"required CSV file is empty",null,true)); return List.of(); }
        try (Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8); CSVParser parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).build().parse(reader)) {
            List<String> headers=parser.getHeaderNames(); Set<String> seen=new HashSet<>();
            for (String h:headers) if (!seen.add(h)) findings.add(fileFinding(role,ValidationErrorCode.HEADER_DUPLICATE,"duplicate header is not allowed",h,true));
            for (String h:required) if (!headers.contains(h)) findings.add(new ValidationFinding(role,null,h,FindingSeverity.ERROR,ValidationErrorCode.HEADER_MISSING.name(),"required header is missing",null,true));
            if (findings.stream().anyMatch(f->f.fileRole()==role && f.blocking() && (f.errorCode().equals(ValidationErrorCode.HEADER_MISSING.name())||f.errorCode().equals(ValidationErrorCode.HEADER_DUPLICATE.name())))) return List.of();
            List<T> result=new ArrayList<>(); int row=0;
            for (CSVRecord record:parser) {
                row++; SourceRef source=new SourceRef(role,row,null); warnOnSentinels(record,source,findings);
                // Commons CSV omits trailing empty cells from a record.  Those are valid optional
                // fields; an extra cell, however, cannot be mapped safely.
                if (record.size()>headers.size() && hasNonBlankExtra(record, headers.size())) { findings.add(finding(source,null,FindingSeverity.ERROR,ValidationErrorCode.CSV_STRUCTURE_INVALID,"CSV record has more fields than its header",null,true)); continue; }
                T value=mapper.map(record,source);
                if (missingIdentifier(value,role)) {
                    findings.add(finding(source,role==FileRole.ISSUES?"key":role==FileRole.SPRINTS?"sprintId":"sprintId/issueKey",FindingSeverity.ERROR,ValidationErrorCode.REQUIRED_IDENTIFIER_BLANK,"required identifier is blank",null,false));
                    quarantine.add(new QuarantinedRecord(source, "required identifier is blank",
                            value instanceof IssueSprintLink link ? link.linkId() : null));
                } else result.add(value);
            }
            if (row==0) findings.add(fileFinding(role,ValidationErrorCode.FILE_EMPTY,"CSV contains a header but no data records",null,true));
            return result;
        } catch (IOException ex) { findings.add(fileFinding(role,ValidationErrorCode.FILE_UNREADABLE,"CSV file could not be read",null,true)); return List.of(); }
          catch (RuntimeException ex) { findings.add(fileFinding(role,ValidationErrorCode.CSV_STRUCTURE_INVALID,"CSV structure could not be parsed",null,true)); return List.of(); }
    }
    private static boolean missingIdentifier(Object v,FileRole role) { if(role==FileRole.ISSUES)return ((Issue)v).issueKey()==null; if(role==FileRole.SPRINTS)return ((Sprint)v).sprintId()==null; IssueSprintLink l=(IssueSprintLink)v; return l.sprintId()==null||l.issueKey()==null; }
    private static boolean hasNonBlankExtra(CSVRecord record, int headerCount) { for (int i=headerCount;i<record.size();i++) if (!record.get(i).trim().isEmpty()) return true; return false; }

    private static List<Issue> resolveIssues(List<Issue> in,List<ValidationFinding> fs,List<QuarantinedRecord> q) { Map<String,Issue> m=new LinkedHashMap<>(); for(Issue x:in){Issue f=m.putIfAbsent(x.issueKey(),x);if(f==null)continue;boolean same=issueEquals(f,x);fs.add(finding(x.sourceRef(),"key",same?FindingSeverity.WARNING:FindingSeverity.ERROR,same?ValidationErrorCode.DUPLICATE_MASTER_IDENTICAL:ValidationErrorCode.DUPLICATE_MASTER_CONFLICT,same?"identical duplicate issue; first record kept":"conflicting duplicate issue; first record kept",x.issueKey(),false));q.add(new QuarantinedRecord(x.sourceRef(),same?"identical duplicate issue":"conflicting duplicate issue"));}return new ArrayList<>(m.values()); }
    private static List<Sprint> resolveSprints(List<Sprint> in,List<ValidationFinding> fs,List<QuarantinedRecord> q) { Map<String,Sprint> m=new LinkedHashMap<>(); for(Sprint x:in){Sprint f=m.putIfAbsent(x.sprintId(),x);if(f==null)continue;boolean same=sprintEquals(f,x);fs.add(finding(x.sourceRef(),"sprintId",same?FindingSeverity.WARNING:FindingSeverity.ERROR,same?ValidationErrorCode.DUPLICATE_MASTER_IDENTICAL:ValidationErrorCode.DUPLICATE_MASTER_CONFLICT,same?"identical duplicate sprint; first record kept":"conflicting duplicate sprint; first record kept",x.sprintId(),false));q.add(new QuarantinedRecord(x.sourceRef(),same?"identical duplicate sprint":"conflicting duplicate sprint"));}return new ArrayList<>(m.values()); }
    private static boolean issueEquals(Issue a,Issue b){return Objects.equals(a.issueKey(),b.issueKey())&&Objects.equals(a.summary(),b.summary())&&Objects.equals(a.issueType(),b.issueType())&&Objects.equals(a.status(),b.status())&&Objects.equals(a.storyPoint(),b.storyPoint())&&Objects.equals(a.priority(),b.priority())&&Objects.equals(a.assignee(),b.assignee())&&Objects.equals(a.description(),b.description())&&Objects.equals(a.creator(),b.creator())&&Objects.equals(a.reporter(),b.reporter())&&Objects.equals(a.watchCount(),b.watchCount())&&Objects.equals(a.commentCount(),b.commentCount())&&Objects.equals(a.votes(),b.votes())&&Objects.equals(a.fixVersions(),b.fixVersions())&&Objects.equals(a.affectedVersions(),b.affectedVersions())&&Objects.equals(a.issueLinks(),b.issueLinks())&&Objects.equals(a.blockedBy(),b.blockedBy())&&Objects.equals(a.blocks(),b.blocks())&&Objects.equals(a.dependedOnBy(),b.dependedOnBy())&&Objects.equals(a.dependedOn(),b.dependedOn())&&Objects.equals(a.subtasks(),b.subtasks());}
    private static boolean sprintEquals(Sprint a,Sprint b){return Objects.equals(a.sprintId(),b.sprintId())&&Objects.equals(a.name(),b.name())&&Objects.equals(a.state(),b.state())&&Objects.equals(a.startDate(),b.startDate())&&Objects.equals(a.endDate(),b.endDate())&&Objects.equals(a.completeDate(),b.completeDate())&&Objects.equals(a.lengthDays(),b.lengthDays())&&Objects.equals(a.developerCount(),b.developerCount())&&Objects.equals(a.recordedIssueCount(),b.recordedIssueCount())&&Objects.equals(a.recordedCompletedCount(),b.recordedCompletedCount())&&Objects.equals(a.recordedNotCompletedCount(),b.recordedNotCompletedCount())&&Objects.equals(a.recordedPuntedCount(),b.recordedPuntedCount())&&Objects.equals(a.recordedCompletedElsewhereCount(),b.recordedCompletedElsewhereCount())&&Objects.equals(a.recordedAddedIssueKeys(),b.recordedAddedIssueKeys())&&Objects.equals(a.estimateSums(),b.estimateSums());}
    private static void warnOnSentinels(CSVRecord r,SourceRef s,List<ValidationFinding> fs){for(String h:r.getParser().getHeaderNames()){if(!r.isSet(h))continue;String v=r.get(h).trim();if(Set.of("na","n/a","null","none","-").contains(v.toLowerCase()))fs.add(finding(s,h,FindingSeverity.WARNING,ValidationErrorCode.SENTINEL_UNKNOWN,"unknown sentinel-like value retained as literal text",v,false));}}
    private static ValidationFinding fileFinding(FileRole r,ValidationErrorCode c,String msg,String raw,boolean blocking){return new ValidationFinding(r,null,null,FindingSeverity.ERROR,c.name(),msg,raw,blocking);}
    private static ValidationFinding finding(SourceRef s,String field,FindingSeverity sev,ValidationErrorCode c,String msg,String raw,boolean blocking){return new ValidationFinding(s.fileRole(),s.recordNumber(),field,sev,c.name(),msg,raw,blocking);}
    private static String text(CSVRecord r,String h){if(!r.isMapped(h)||!r.isSet(h))return null;String v=r.get(h).trim();return v.isEmpty()?null:v;}
    private static List<String> listValue(CSVRecord r,String h){String v=text(r,h);return v==null?null:List.of(v);}
    private static BigDecimal decimal(CSVRecord r,String h,SourceRef s,List<ValidationFinding> fs){String v=text(r,h);if(v==null)return null;try{BigDecimal n=new BigDecimal(v);if(n.signum()<0){fs.add(finding(s,h,FindingSeverity.ERROR,ValidationErrorCode.NUMBER_NEGATIVE,"negative numeric value is prohibited and was left absent",v,false));return null;}return n;}catch(NumberFormatException e){fs.add(finding(s,h,FindingSeverity.WARNING,ValidationErrorCode.NUMBER_INVALID,"optional numeric value is invalid and was left absent",v,false));return null;}}
    private static Integer integer(CSVRecord r,String h,SourceRef s,List<ValidationFinding> fs){String v=text(r,h);if(v==null)return null;try{int n=Integer.parseInt(v);if(n<0){fs.add(finding(s,h,FindingSeverity.ERROR,ValidationErrorCode.NUMBER_NEGATIVE,"negative numeric value is prohibited and was left absent",v,false));return null;}return n;}catch(NumberFormatException e){fs.add(finding(s,h,FindingSeverity.WARNING,ValidationErrorCode.NUMBER_INVALID,"optional integer value is invalid and was left absent",v,false));return null;}}
    private static LocalDate date(CSVRecord r,String h,SourceRef s,List<ValidationFinding> fs){String v=text(r,h);if(v==null)return null;try{return LocalDate.parse(v,DATE_FORMAT);}catch(DateTimeParseException e){fs.add(finding(s,h,FindingSeverity.WARNING,ValidationErrorCode.DATE_INVALID,"optional date is invalid and was left absent",v,false));return null;}}
    private List<FileFingerprint> fingerprints() throws IOException {List<FileFingerprint> out=new ArrayList<>();addFingerprint(out,FileRole.ISSUES,properties.issuesPath());addFingerprint(out,FileRole.ISSUE_SPRINT_SUMMARY,properties.issueSprintSummaryPath());addFingerprint(out,FileRole.SPRINTS,properties.sprintsPath());return out;}
    private static void addFingerprint(List<FileFingerprint> out,FileRole r,Path p)throws IOException{if(!Files.exists(p)||!Files.isRegularFile(p))return;try{out.add(new FileFingerprint(r,p.getFileName().toString(),Files.size(p),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)))));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 is unavailable",e);}}
    @FunctionalInterface private interface RecordMapper<T>{T map(CSVRecord record,SourceRef source);}
}
