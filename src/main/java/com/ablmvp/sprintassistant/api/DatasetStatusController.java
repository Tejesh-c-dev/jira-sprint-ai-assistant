package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.DatasetStatusResponse;
import com.ablmvp.sprintassistant.api.dto.DatasetQualityResponse;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.data.result.DatasetLoadOutcome;
import com.ablmvp.sprintassistant.domain.FileRole;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.IssueSprintLink;
import com.ablmvp.sprintassistant.domain.Sprint;
import java.util.LinkedHashSet;
import java.util.Set;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dataset")
@Tag(name = "Dataset")
public class DatasetStatusController {
    private final LoadResult loadResult;

    public DatasetStatusController(LoadResult loadResult) {
        this.loadResult = loadResult;
    }

    @GetMapping("/status")
    @Operation(summary = "Get the loaded dataset status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Load outcome and usability returned"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public DatasetStatusResponse status() {
        long blockingCount = loadResult.findings().stream()
                .filter(finding -> finding.blocking())
                .count();
        return new DatasetStatusResponse(
                loadResult.loadOutcome(),
                loadResult.loadOutcome() == DatasetLoadOutcome.LOADED
                        || loadResult.loadOutcome() == DatasetLoadOutcome.PARTIALLY_LOADED,
                loadResult.usabilityState(),
                loadResult.findings().size(),
                (int) blockingCount,
                loadResult.quarantinedRecords().size(),
                ApiMapper.toProvenance(loadResult.provenance()));
    }

    @GetMapping("/quality")
    @Operation(summary = "Get relationship and duplicate dataset quality counts")
    public DatasetQualityResponse quality() {
        Set<String> issueKeys = new LinkedHashSet<>();
        for (Issue issue : loadResult.issues()) if (issue.issueKey() != null) issueKeys.add(issue.issueKey());
        Set<String> sprintIds = new LinkedHashSet<>();
        for (Sprint sprint : loadResult.sprints()) if (sprint.sprintId() != null) sprintIds.add(sprint.sprintId());

        int unknownSprint = 0;
        int unknownIssue = 0;
        for (IssueSprintLink link : loadResult.issueSprintLinks()) {
            if (link.sprintId() != null && !sprintIds.contains(link.sprintId())) unknownSprint++;
            if (link.issueKey() != null && !issueKeys.contains(link.issueKey())) unknownIssue++;
        }
        int duplicateIssue = (int) loadResult.quarantinedRecords().stream()
                .filter(record -> record.sourceRef().fileRole() == FileRole.ISSUES)
                .count();
        long blockingCount = loadResult.findings().stream()
                .filter(finding -> finding.blocking()).count();
        return new DatasetQualityResponse(
                loadResult.loadOutcome(), loadResult.usabilityState(),
                loadResult.findings().size(), (int) blockingCount,
                loadResult.quarantinedRecords().size(),
                unknownSprint, unknownIssue, duplicateIssue,
                ApiMapper.toProvenance(loadResult.provenance()));
    }
}
