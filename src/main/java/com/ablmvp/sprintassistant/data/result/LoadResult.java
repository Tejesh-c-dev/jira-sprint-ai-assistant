package com.ablmvp.sprintassistant.data.result;

import com.ablmvp.sprintassistant.domain.DatasetProvenance;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.IssueSprintLink;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.domain.UsabilityState;

import java.util.List;

public record LoadResult(
        List<Issue> issues,
        List<Sprint> sprints,
        List<IssueSprintLink> issueSprintLinks,
        List<QuarantinedRecord> quarantinedRecords,
        List<ValidationFinding> findings,
        DatasetProvenance provenance,
        UsabilityState usabilityState) {

    public LoadResult {
        issues = List.copyOf(issues == null ? List.of() : issues);
        sprints = List.copyOf(sprints == null ? List.of() : sprints);
        issueSprintLinks = List.copyOf(issueSprintLinks == null ? List.of() : issueSprintLinks);
        quarantinedRecords = List.copyOf(quarantinedRecords == null ? List.of() : quarantinedRecords);
        findings = List.copyOf(findings == null ? List.of() : findings);
    }
}
