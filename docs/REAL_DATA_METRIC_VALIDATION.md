# Real Data Metric Validation

Status: **PARTIAL REAL-DATA VALIDATION — 2026-11-18**

## Verified live load

- 1,992 issue rows loaded into parsing scope.
- 1,902 distinct issue keys after duplicate quarantine; 90 rows quarantined from master duplicates.
- 2,861 issue-sprint link rows retained, subject to unknown-issue warnings.
- 66 sprint rows loaded.
- 599 unknown issue references.
- 0 unknown sprint references.
- Dataset usability: `USABLE_WITH_WARNINGS`.
- Load outcome: `LOADED`.
- Quarantined records: 90.
- Findings: 1,259, with 0 blocking findings.

## Source facts

### Recorded counts

Verified as source facts from `Sprint` fields:

- `recordedIssueCount`
- `recordedCompletedCount`
- `recordedNotCompletedCount`
- `recordedPuntedCount`
- `recordedCompletedElsewhereCount`

These fields are exposed unchanged and must not be replaced with derived link counts.

### Individual estimate sums

All eight individual `SprintEstimateSums` fields exist with no source blanks.
Units and initial/current semantics remain unresolved.
Status: **PROVISIONAL**.

### Sprint length

`SprintLength` is source-recorded and verified for all 66 sprints.
Status: **VERIFIED**.

### Added issue keys

`issueKeysAddedDuringSprint` is present for every sprint.
Its encoding is not yet independently profiled as a list; the loader currently exposes it as a
single source string/list representation.
Status: **VERIFIED WITH WARNINGS**.

## Distinct issue membership

`(sprintId, issueKey)` link pairs contain no repeated pairs.
Unknown issue references are retained as links but excluded from known-issue distinct counts.

Sprint 1 example:

- 16 distinct known issue keys
- `UNKNOWN_ISSUE_REFERENCE` warning

Status: **VERIFIED WITH WARNINGS**.

## Assignee workload

Assignee is available on both issue and link records.
The live data contains conflicting issue/link observations in Sprint 1 and missing issue
assignees in 2 issue-master rows.

The implementation preserves link-first, issue-fallback precedence and conflict evidence.
No estimate-weighted workload is implemented.

Status: **PROVISIONAL**.

## Unsupported metrics

- Completion rate: unavailable pending overlap/denominator evidence.
- Carryover count/rate: unavailable; incomplete or cross-sprint membership is not carryover proof.
- Scope-change rate: unavailable; added-key encoding and scope denominator are unresolved.
- Combined estimate totals: unavailable; units/semantics not established.
- Health classification: unavailable; no thresholds and incomplete evidence.

## Phase 2D real-data reconciliation

### Recorded issue counts

The source total sums to 3,539 across all sprints. The four source count categories sum to
different source totals in 63 sprints. Known issue-key membership differs from the source
`totalNumberOfIssues` in 64 of 66 sprints in the first reconciliation. Differences are
established; no matching formula is inferred.

### Completion

The issue master status has only `Done`, `To Do` and `In Progress`. It must not be assumed that
`Done` equals the sprint summary's `Completed` category.

The summary status vocabulary is `Completed`, `Punted`, `CompletedInNotherSprint` and
`NotCompletedWithinSprint`. Category overlap and denominator semantics remain unresolved.

Status: **REQUIRES_HUMAN_DECISION**.

### Story points

Real issue `storyPoint` values include 0 and range through 40. Summary initial/current values
are decimals and include -1 in one initial-story-point value.

- Initial and current summary totals: 9,826.2 and 9,795.2.
- Initial/current differs for 158 link rows.
- Units and whether initial/current are comparable across issue/link/sprint levels remain unknown.

Status: **PROVISIONAL**.

### Assignee workload

Issue master and link assignees use display names and usernames respectively in many observed
rows. The 2,202 textual differences are not necessarily conflicts; a crosswalk is not established.

There are 214 literal `null` link assignee values, and 2 issue-master assignee values are missing.
No unresolved assignee policy is changed.

Status: **VERIFIED_WITH_WARNINGS** for raw evidence; **REQUIRES_HUMAN_DECISION** for a resolved policy.

### Scope additions

`issueKeysAddedDuringSprint` has no blanks and 35 distinct source values. It is currently exposed
as a source string/list representation. No comma encoding was observed.

Status: **PROVISIONAL**.

### Carryover

341 known issue keys appear in multiple sprints, but no sprint-order rule or completion
crosswalk establishes actual carryover.

Status: **UNAVAILABLE**.
