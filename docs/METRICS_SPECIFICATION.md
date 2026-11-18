# Sprint Metrics Specification

Status: **DESIGN ONLY — awaiting human approval**

This document specifies the sprint metrics that may be derived from the current normalized
model. It does not change the loader, domain records or API.

The mappings in `docs/DATA_MAPPING.md` and `docs/DATA_MODEL.md` remain **provisional** until real
CSV bytes are available. Synthetic fixture values must not be used to validate metric semantics.
They may only be used to test implementation behavior after the team approves a metric contract.

## 1. Metric availability contract

Every metric should carry a value and an availability status:

```text
MetricResult<T> {
  value: T | null
  availability: AVAILABLE | AVAILABLE_WITH_WARNINGS | UNAVAILABLE | INSUFFICIENT_DATA | PROVISIONAL | REQUIRES_HUMAN_DECISION
  reasons: string[]
  sourceFields: string[]
  findingCodes: string[]
}
```

The exact Java/API types are implementation decisions for Phase 1B. This specification defines
behavior, not implementation classes.

### AVAILABLE

Use when:

- the required fields exist;
- the formula is agreed;
- all required values are present;
- zero denominators do not invalidate the metric;
- there are no relevant data-quality findings.

### AVAILABLE_WITH_WARNINGS

Use when the metric is calculable, but:

- a non-blocking finding affects coverage or interpretation;
- duplicate sprint/issue pairs were retained;
- unknown issue references are retained;
- an optional value is missing outside the metric's required denominator; or
- the result is based on source aggregates rather than independently verified relationships.

The warning must identify the finding or coverage limitation. It must not silently change the
value.

### UNAVAILABLE

Use when the metric is unsupported by the current model or source semantics, a required field
or mapping is not present, or a team decision has not authorized the formula.

`UNAVAILABLE` is not the same as a value of zero.

### INSUFFICIENT_DATA

Use when the metric is supported in principle but required source values are absent or a
denominator is zero. The result must remain `null`.

For example:

- a missing or null numerator/denominator;
- a denominator that is zero;
- a missing required sprint date;
- missing estimates needed for an agreed estimate metric; or
- no sprint/issue links from which to derive a distinct-issue count.

An empty link set is insufficient for derived coverage, unless a source aggregate has a separate
contract that proves coverage.

## 2. Global missing-data and integrity rules

1. `null` means unavailable. It must not be replaced with zero.
2. A parsed numeric zero is zero and must not be confused with `null`.
3. Source counts and source estimate sums remain source facts. Metrics must not replace,
   back-fill or recompute them by default.
4. Status strings remain raw. They must not be interpreted as completed or incomplete until the
   team approves an explicit mapping.
5. An issue-sprint pair or `issueKey` count must be distinct by `issueKey` per sprint.
   `IssueSprintLink.linkId` identifies individual source rows but is not a distinct-issue count.
6. Unknown sprint links are quarantined and unavailable to sprint metrics.
7. Unknown issue links are retained with an unknown-issue warning. They may contribute to a
   distinct-link count if an approved definition says that source-level coverage includes them,
   but they must not be presented as normalized `Issue` records.
8. Repeated sprint/issue link rows remain separate records. Any distinct-issue formula must
   de-duplicate by `(sprintId, issueKey)` without removing source provenance.
9. Conflicting or identical duplicate issue/sprint master rows have already been resolved by the
   loader. Metrics use the retained master record.
10. Blocking usability findings make input data unsafe for metric interpretation.
11. Relevant warnings must be copied into metric or sprint analysis output. The loader's
    `UsabilityState` describes input data quality, not sprint health.

## 2.1 Finding scope

Validation findings have two scopes:

- **Sprint-specific** findings are associated by matching `ValidationFinding.fileRole` and
  `ValidationFinding.recordNumber` to the retained record's `SourceRef.fileRole` and
  `SourceRef.recordNumber`.
- **Dataset-level** findings have no precise row association, typically because
  `ValidationFinding.recordNumber` is `null`. They apply to every sprint.

Membership findings support a stronger association:

- `UNKNOWN_ISSUE_REFERENCE` findings are created for a link source row and are associated with
  that row's `IssueSprintLink.sprintId`.
- `DUPLICATE_LINK_PAIR` findings are created from the first link source row for that pair and are
  associated with that row's `IssueSprintLink.sprintId`.

If a record-numbered finding cannot be associated confidently with a retained sprint or link,
the finding must remain dataset-level evidence rather than be silently dropped. This conservative
fallback may affect more than one sprint, but it does not invent a sprint identity.

## 3. Metric catalogue

## 3.1 Total distinct issues per sprint

**Business definition:** The number of unique `issueKey` values linked to one sprint.

**Formula, if supported:**

```text
countDistinct(issueKey) for issueSprintLinks where sprintId = selectedSprintId
```

**Required fields**

- `IssueSprintLink.sprintId`
- `IssueSprintLink.issueKey`
- `IssueSprintLink.linkId` for source-row traceability

**Field availability:** Supported by `IssueSprintLink`; also influenced by `LoadResult.issues`.

**Null handling**

- Links with null `sprintId` or `issueKey` are quarantined.
- Unknown issue keys are represented as links and may be counted only under an explicitly
  approved source-level definition.
- An empty de-duplicated set may produce `INSUFFICIENT_DATA`, not zero, unless the source
  aggregate separately establishes coverage.

**Zero denominator behavior:** This is a count, not a rate. No denominator is required. If zero
is an established source fact rather than missing data, it may be displayed as zero only if the
team separately decides that the link coverage is complete.

**Duplicate/repeated-link handling:** Repeated `(sprintId, issueKey)` rows are counted once.
The count must not use `linkId` as issue identity.

**Data-quality dependencies:** Unknown-sprint links are excluded. Unknown-issue links add
`UNKNOWN_ISSUE_REFERENCE` warnings. `DUPLICATE_LINK_PAIR` affects interpretation and availability
but not the distinct count.

**Status:** **PROVISIONAL / source coverage agreement required**

This metric is mechanically supported, but no real evidence proves that the summary link table
covers every issue in each sprint.

## 3.2 Recorded issue count

**Business definition:** The sprint producer's recorded total number of issues.

**Required fields**

- `Sprint.recordedIssueCount`
- `Sprint.sprintId`

**Formula:** None. Return the source value unchanged.

**Field availability:** Available if the field is non-null.

**Null handling:** `null` produces `INSUFFICIENT_DATA`. Do not derive the value from link rows.

**Zero denominator behavior:** A real source zero is allowed. Its meaning must remain a source
fact; it must not be interpreted as evidence that the sprint had no issues unless source coverage
supports that interpretation.

**Duplicate/repeated-link handling:** No de-duplication is applied. This is not a calculated
link count.

**Data-quality dependencies:** Preserve the field as an independent source fact. A warning should
be added if an approved comparison differs from a distinct-link count.

**Status:** **SUPPORTABLE IMMEDIATELY** with the source field as raw factual output, but
**PROVISIONAL** for real-source semantics.

## 3.3 Completed, incomplete and punted issue counts

**Business definition:** Source-producer categories for sprint issue outcomes.

**Required fields**

- `Sprint.recordedCompletedCount`
- `Sprint.recordedNotCompletedCount`
- `Sprint.recordedPuntedCount`
- `Sprint.recordedCompletedElsewhereCount`

**Formula:** None. Return each non-null source field unchanged.

**Field availability:** Each field is independently optional.

**Null handling:** Each missing field produces `INSUFFICIENT_DATA` for that field only. Missing
fields must not be zero-filled or omitted as if they were present.

**Zero denominator behavior:** Not applicable to individual counts. A present zero is preserved.

**Duplicate/repeated-link handling:** Source aggregates are not re-derived from
`IssueSprintLink`, and repeated links must not change these values.

**Data-quality dependencies:** Real-source category definitions and overlap are unresolved.
The loader does not map status strings into these categories.

**Status:** **SUPPORTED AS SOURCE FACTS**; completed/incomplete derivation from issue statuses
is **UNAVAILABLE**.

## 3.4 Story-point totals

**Business definition:** Sprint or issue estimate totals in source units.

**Required fields**

Source aggregate options:

- `Sprint.estimateSums.completedInitial`
- `Sprint.estimateSums.completedCurrent`
- `Sprint.estimateSums.puntedInitial`
- `Sprint.estimateSums.puntedCurrent`
- `Sprint.estimateSums.notCompletedInitial`
- `Sprint.estimateSums.notCompletedCurrent`
- `Sprint.estimateSums.completedElsewhereInitial`
- `Sprint.estimateSums.completedElsewhereCurrent`

Per-issue options:

- `IssueSprintLink.initialStoryPoint`
- `IssueSprintLink.currentStoryPoint`
- `Issue.storyPoint`

**Formula:** No approved total formula is defined.

If a team later chooses a distinct explicit source, a sum may include only non-null values
belonging to that source. Missing values must not be coerced to zero.

**Field availability:** The fields exist but their meanings and units are unresolved.

**Null handling:** An individual missing estimate means unavailable or partial coverage. An
estimate sum metric is `INSUFFICIENT_DATA` unless the approved formula defines how omitted
values are handled. The model must not assume that null means zero.

**Zero denominator behavior:** A story-point denominator is not always present. Zero must be
marked as zero by the source, not by coercion.

**Duplicate/repeated-link handling:** Repeated `(sprintId, issueKey)` rows must not be summed
multiple times if an issue-level source is selected. Initial/current link values are
per-sprint observations and must not be merged across sprints.

**Data-quality dependencies:** Invalid or negative values have already become `null` with
findings. Unknown links and duplicate pairs affect coverage.

**Status:** **UNAVAILABLE** for calculated or cross-field totals. Individual
`Sprint.estimateSums` values may be returned as source facts.

## 3.5 Sprint completion rate

**Business definition:** A percentage of source issue completion for the sprint.

**Potential formula:** `completedCount / recordedIssueCount`, but this formula is only a
candidate. It must not be presented as approved until category overlap and source coverage are
decided.

**Required fields**

- `Sprint.recordedCompletedCount`
- `Sprint.recordedIssueCount`
- Or an approved alternative source for both numerator and denominator

**Field availability:** Both fields exist as optional integers.

**Null handling:** Missing either value produces `INSUFFICIENT_DATA`.

**Zero-denominator behavior:** If `recordedIssueCount == 0`, return
`INSUFFICIENT_DATA`, not 0% and not 100%.

**Duplicate/repeated-link handling:** No use of link rows until a team decides that derived
counts are an approved alternative to source aggregates.

**Data-quality dependencies:** Unknown or duplicate source categories, `UNUSABLE` input and
whether complete/elsewhere/not-completed counts partition the issue count are unresolved.

**Status:** **UNAVAILABLE until formula approval**.

## 3.6 Carryover rate

**Business definition:** The proportion of issues carried from a prior sprint or not completed
in the current sprint.

**Formula:** No approved formula exists. `recordedNotCompletedCount` is a source count but
must not be labeled carryover without historical evidence.

**Required fields**

- A historical issue-sprint link sequence; or
- An approved source field that explicitly identifies carryover
- Plus an agreed denominator

**Field availability:** `IssueSprintLink` can show issue membership across links, but it does
not establish chronological or unambiguous carryover.

**Null handling:** `null` or empty historical links yield `INSUFFICIENT_DATA`.

**Zero-denominator behavior:** A zero approved denominator yields `INSUFFICIENT_DATA`.

**Duplicate/repeated-link handling:** Multiple repeated pairs must be collapsed before checking
distinct issue membership. Historical adjacency and completion evidence are still required.

**Data-quality dependencies:** Sprint ordering, status semantics and duplicate pair/cardinality
behavior are unresolved.

**Status:** **UNAVAILABLE**.

## 3.7 Scope changes

**Business definition:** The sprint's explicitly recorded added issues.

**Required fields**

- `Sprint.recordedAddedIssueKeys`
- `Sprint.recordedIssueCount`
- `Sprint.startDate` and `endDate` only if a time-bounded definition is approved

**Field availability:** `recordedAddedIssueKeys` exists as `List<String>` or `null`.

**Formula candidates**

- count of distinct nonblank keys: `countDistinct(recordedAddedIssueKeys)`
- scope-added proportion: added issue count / approved sprint scope denominator

No scope-change formula is approved because the source encoding is unresolved.

**Null handling:** `null` means unavailable. An empty list may mean no recorded additions only
if source encoding proves that distinction.

**Zero denominator behavior:** A missing or zero approved denominator produces
`INSUFFICIENT_DATA`.

**Duplicate/repeated-key handling:** De-duplicate added keys when counting distinct issues.
The exact delimiter and source encoding must be profiled first.

**Data-quality dependencies:** The meaning of `issueKeysAddedDuringSprint`, sentinel encoding
and overlap with other recorded issue counts are unresolved.

**Status:** **PROVISIONAL / unavailable for real source**.

## 3.8 Assignee workload distribution

**Business definition:** A distribution of sprint issue or estimate responsibility by assignee.

**Required fields**

- `IssueSprintLink.assignee`
- `Issue.assignee` as fallback
- `IssueSprintLink.sprintId`
- `IssueSprintLink.issueKey`
- `IssueSprintLink.initialStoryPoint` or `currentStoryPoint` only for estimate-weighted workload

**Field availability:** Available text fields exist, but completeness is not guaranteed.

**Formula, if supported**

Issue count by assignee:

```text
for each sprint, countDistinct(issueKey) grouped by resolved assignee
```

Resolved assignee uses:

1. `IssueSprintLink.assignee`
2. `Issue.assignee`
3. no assignee if both are null

The fallback and precedence are already defined in the normalized data model.

**Null handling:** Null assignee is not “Unknown” unless the team defines an output category.
Estimate-weighted workload must not sum missing estimates as zero.

**Zero-denominator behavior:** Not applicable to raw issue counts. If percentages are later
requested, a zero total issue count gives `INSUFFICIENT_DATA`.

**Duplicate/repeated-link handling:** Count each distinct `(sprintId, issueKey)` once. If link
rows disagree on assignee, preserve all source details and apply the approved precedence without
silently overwriting evidence.

**Data-quality dependencies:** Unknown issue links lack a fallback master. Conflicting link
assignee values, unassigned issues and source coverage require decisions.

**Status:** **PROVISIONAL** for issue-count workload; estimate-weighted workload is
**UNAVAILABLE**.

**Conflict policy:** Conflicting repeated link-level assignees have no approved resolution. The
implementation must preserve every observed assignee and must not assign the issue to an invented
assignee. If exactly one link-level assignee is present, it takes precedence over the issue-level
assignee for the distribution; the conflict remains recorded in evidence. If both assignee sources
are missing, the issue is unresolved and is not placed under an invented category.

## 3.9 Sprint duration and planned versus actual duration

**Business definition:** Calendar span and comparison of recorded versus observed sprint
duration.

**Required fields**

- `Sprint.startDate`
- `Sprint.endDate`
- `Sprint.completeDate`
- `Sprint.lengthDays`

**Formula candidates**

```text
plannedDuration = sprint.endDate - sprint.startDate
actualDuration = sprint.completeDate - sprint.startDate
```

The definition of inclusive versus exclusive day counts must be approved. The source
`lengthDays` is a separate recorded field and must not be overwritten.

**Field availability:** All fields exist but are optional.

**Null handling:** Missing required dates produce `INSUFFICIENT_DATA`. Missing
`lengthDays` does not prevent date-derived duration until the team decides precedence.

**Zero-denominator behavior:** Duration is not a rate.

**Duplicate/repeated-link handling:** Not applicable.

**Data-quality dependencies:** Invalid dates already become `null`. `completeDate` may be
absent for a sprint state whose semantics are unresolved.

**Status:** **PROVISIONAL**. `lengthDays` may be exposed immediately as a source fact.
Date-derived planned/actual duration requires inclusive/exclusive approval.

## 3.10 Sprint health classification

**Business definition:** A project-defined label intended to summarize sprint delivery risk.

**Required fields:** To be decided. Candidate inputs include:

- `Sprint.recordedIssueCount`
- `Sprint.recordedCompletedCount`
- `Sprint.recordedNotCompletedCount`
- `Sprint.recordedPuntedCount`
- `Sprint.recordedCompletedElsewhereCount`
- approved estimates or estimate-derived values
- approved scope-change values
- sprint duration
- relevant `LoadResult.findings`

**Formula:** None approved.

**Field availability:** Raw fields exist, but no status mapping or threshold model exists.

**Null handling:** Health must be `INSUFFICIENT_DATA` when required evidence is missing. It must
not default to healthy, watch or at-risk.

**Zero-denominator behavior:** No candidate denominator may produce a health label when it is
zero or unknown.

**Duplicate/repeated-link handling:** Health must use approved source semantics, not raw link
counts unless the team explicitly defines a derived health model.

**Data-quality dependencies:** Blocking findings should prevent an analytical health label.
Warnings may produce `AVAILABLE_WITH_WARNINGS` only if the health formula is defined and the
warning does not invalidate the required evidence.

**Status:** **UNAVAILABLE until team approval**.

## 4. Missing-data policy

The metrics layer must distinguish:

| Situation | Policy |
|---|---|
| Required source field is `null` | `INSUFFICIENT_DATA` |
| Required source field is absent or mapping is unverified | `UNAVAILABLE` |
| Formula is not approved | `UNAVAILABLE` |
| Numerator or denominator is missing | `INSUFFICIENT_DATA` |
| Denominator is zero | `INSUFFICIENT_DATA` for a rate |
| Optional detail is missing but metric core remains valid | `AVAILABLE_WITH_WARNINGS` or partial result, as approved |
| Finding is blocking | Metric unavailable or input unusable |
| Finding is warning-only | Preserve `AVAILABLE_WITH_WARNINGS` when the metric is valid |
| Repeated link pair | Preserve source rows and mark duplicate warning; distinct-issue metrics de-duplicate |
| Unknown sprint reference | Loader excludes the link |
| Unknown issue reference | Retain the link as data, expose the warning and never invent an `Issue` master |

For membership-derived metrics, `INSUFFICIENT_DATA` has two meanings that must remain explicit:

- **Truly empty sprint:** no known Issue membership rows were found and no unknown-member evidence
  exists. The result is insufficient, not zero.
- **Incomplete membership:** an unknown Issue reference or unmatched coverage evidence exists.
  The result is insufficient and must include the relevant finding code and reason. Unknown Issue
  references must never be converted into counts.

## 5. Edge cases

### 5.1 Repeated links

The loader returns every accepted `IssueSprintLink` row and a `DUPLICATE_LINK_PAIR` finding.
The metric layer must preserve the rows in source detail and de-duplicate them only for
distinct-issue counts.

### 5.2 Conflict between source aggregates and link rows

No value precedence is approved. Source aggregates must remain separate, and mismatches should
be represented as warnings or diagnostic data when the team defines a reconciliation rule.

### 5.3 Unknown issue links

An unknown issue key is not an invented issue. It may be counted as distinct source membership
only under an approved source-coverage definition.

### 5.4 Missing estimates

Missing estimates are not zero. Partial sums may be labeled unavailable or partial only under an
explicitly approved policy.

### 5.5 Status strings

Raw `Issue.status` and `IssueSprintLink.status` values must not be classified. A status table
must be approved from real data or documented source evidence before derivation.

### 5.6 Sprint with no links

A sprint may remain in `LoadResult.sprints` with no accepted links. It does not prove that the
sprint has zero issues. Source aggregate fields decide only if the sprint coverage contract
supports their use.

### 5.7 Blocking usability

An overall `UNUSABLE` load must not feed a rate or health result. Metrics should be unavailable
until the blocking input issue is corrected.

## 6. Sprint health classification proposal

### 6.1 Legitimate candidate evidence

- source-recorded issue completion category;
- source-recorded incomplete and punted categories;
- source-recorded completed-elsewhere category;
- approved scope-added data;
- approved estimate totals;
- sprint duration evidence;
- data-quality and unknown-reference findings.

### 6.2 Required before implementation

1. An explicit status mapping or confirmation that only source aggregates may contribute.
2. Completion and carryover denominator definitions.
3. Whether incomplete and punted categories overlap.
4. Whether completed-in-another-sprint is separate or partial completion.
5. Estimate semantics and units.
6. Scope-change semantics and coverage.
7. Zero-denominator policy for each rate.
8. Health thresholds and their source of authority.
9. Whether warnings reduce health confidence or only add metadata.
10. Insufficient-data labels and precedence when several evidence dimensions are missing.
11. Whether health is prohibited for synthetic fixtures or allowed only as test behavior.

### 6.3 Proposed model shape

The implementation should use a non-domain-specific health result:

```text
HealthResult {
  state: UNAVAILABLE | INSUFFICIENT_DATA | AVAILABLE_WITH_WARNINGS
  label: string | null
  contributingMetrics: [...]
  reasons: string[]
  findings: [...]
  provenance: DatasetProvenance
}
```

No label should be emitted before the team approves a mapping and thresholds.

## 7. Metric implementation classification

### Can be implemented immediately from the normalized model

With synthetic-fixture tests and no real-source semantic claims:

- return `Sprint.recordedIssueCount` as source data;
- return the five recorded sprint count fields separately;
- return individual `Sprint.estimateSums` values as source facts;
- expose `Sprint.lengthDays` as source data;
- return distinct `issueKey` membership from accepted links;
- produce assignee issue-count groups using the documented fallback precedence;
- expose source `recordedAddedIssueKeys` without interpreting it;
- expose provenance and validation findings.

These implementations must still label provisional source semantics in API/report output.

### Require team decisions

- completion rate;
- carryover rate;
- estimate totals and estimate-weighted workload;
- scope-change count and scope-change rate;
- planned versus actual duration date arithmetic;
- health classification;
- conflict precedence between source aggregates and derived link counts.

### Cannot be supported by the current data

- verified real-source status-to-completion mapping;
- verified real-source carryover inference;
- verified estimate-unit semantics;
- real historical completeness;
- verified scope-addition encoding;
- licensed real-data release;
- approved sprint-health thresholds.

## 8. Unresolved decisions requiring human approval

1. Must the implementation expose only source-recorded metrics before real CSV profiling?
2. Is the distinct issue count allowed to include unknown-issue links?
3. What percentage formula, if any, should be used for completion?
4. Is `recordedNotCompletedCount` carryover, or is carryover strictly a historical sequence?
5. How do punted, not-completed and completed-elsewhere categories overlap?
6. What are the initial/current story-point semantics and units?
7. Can issue and link story-point values be added or compared?
8. How is `issueKeysAddedDuringSprint` encoded and delimited?
9. What statuses count as completed/incomplete?
10. Is planned duration inclusive or exclusive?
11. What evidence is sufficient for a health label?
12. What health thresholds, if thresholds are approved at all?

## 9. Recommended implementation order

1. Add synthetic-only tests for source-fact metrics and null-preservation behavior.
2. Implement a metric-result/availability contract.
3. Implement recorded issue and category count output.
4. Implement individual sprint estimate-sum output without deriving cross-category totals.
5. Implement distinct issues per sprint with `issueKey` de-duplication.
6. Implement assignee issue-count distribution using link-first, issue-fallback precedence.
7. Implement sprint duration source facts and, only after approval, derived date arithmetic.
8. Add mismatch diagnostics separately from primary metrics.
9. Leave completion rate, carryover, scope-rate and health unavailable until decisions are approved.
10. Add REST/CLI only after the metric service contract is accepted.

## 10. No implementation performed

This document defines design only. It does not modify:

- existing Java classes;
- CSV parsing or validation;
- Maven configuration;
- REST endpoints;
- CLI commands; or
- metric calculations.

## 11. Definitive Phase 1C metric catalogue

The contracts in this section are framework-independent. They do not create REST endpoints or
change the Java implementation. Source names are contractual only for the normalized model.

### 11.1 Sprint source facts

| Metric | Exact meaning | Source field | Type/unit | Status |
|---|---|---|---|---|
| Recorded issue count | Sprint producer's total issue count | `Sprint.recordedIssueCount` | nullable integer / issue count | Source-recorded |
| Recorded completed count | Producer's completed category count | `Sprint.recordedCompletedCount` | nullable integer / issue count | Source-recorded |
| Recorded not-completed count | Producer's not-completed-in-sprint category count | `Sprint.recordedNotCompletedCount` | nullable integer / issue count | Source-recorded |
| Recorded punted count | Producer's punted category count | `Sprint.recordedPuntedCount` | nullable integer / issue count | Source-recorded |
| Recorded completed-elsewhere count | Producer's count completed in another sprint | `Sprint.recordedCompletedElsewhereCount` | nullable integer / issue count | Source-recorded |
| Recorded sprint length | Source-recorded sprint length | `Sprint.lengthDays` | nullable integer / days | Source-recorded |
| Recorded added issue keys | Source-recorded key collection | `Sprint.recordedAddedIssueKeys` | nullable list of strings | Source-recorded / encoding provisional |
| Completed initial estimate sum | Individual source sum | `SprintEstimateSums.completedInitial` | nullable `BigDecimal` / source unit | Source-recorded |
| Completed current estimate sum | Individual source sum | `SprintEstimateSums.completedCurrent` | nullable `BigDecimal` / source unit | Source-recorded |
| Punted initial estimate sum | Individual source sum | `SprintEstimateSums.puntedInitial` | nullable `BigDecimal` / source unit | Source-recorded |
| Punted current estimate sum | Individual source sum | `SprintEstimateSums.puntedCurrent` | nullable `BigDecimal` / source unit | Source-recorded |
| Not-completed initial estimate sum | Individual source sum | `SprintEstimateSums.notCompletedInitial` | nullable `BigDecimal` / source unit | Source-recorded |
| Not-completed current estimate sum | Individual source sum | `SprintEstimateSums.notCompletedCurrent` | nullable `BigDecimal` / source unit | Source-recorded |
| Completed-elsewhere initial estimate sum | Individual source sum | `SprintEstimateSums.completedElsewhereInitial` | nullable `BigDecimal` / source unit | Source-recorded |
| Completed-elsewhere current estimate sum | Individual source sum | `SprintEstimateSums.completedElsewhereCurrent` | nullable `BigDecimal` / source unit | Source-recorded |

Common source-fact rules:

1. `null` remains `INSUFFICIENT_DATA`.
2. Integer or `BigDecimal` zero remains a known zero and remains `AVAILABLE` unless another
   relevant warning applies.
3. Empty `recordedAddedIssueKeys` means an empty source collection only when the source field is
   present as an empty list. It is distinct from `null`.
4. Source facts do not derive or replace values from link rows.
5. Sprint-row findings apply to only the matching source row; dataset-level findings apply to all
   sprints. Unassociated record findings remain dataset-level evidence under the conservative
   fallback.

Relevant source-fact fields for estimate values are the eight individual fields above. A metric
must expose one field at a time and must not sum or combine categories.

### 11.2 Distinct issue membership

The definite formula is:

```text
countDistinct(issueKey)
for accepted IssueSprintLink rows
where sprintId = selectedSprintId
and issueKey exists in LoadResult.issues
```

Contract decisions:

- Repeat `(sprintId, issueKey)` rows are not counted repeatedly.
- Unknown sprint links are already quarantined and never enter the count.
- Unknown issue keys are retained in the link collection but excluded unless a future approved
  source-coverage definition explicitly permits them.
- A sprint with no known membership rows and no unknown-member evidence is `INSUFFICIENT_DATA`,
  not an asserted zero.
- A sprint with unknown Issue references or unmappable membership evidence is
  `INSUFFICIENT_DATA`, because coverage is incomplete, and must expose relevant reasons/codes.
- A sprint with known membership and no relevant warning is `AVAILABLE`.
- Known membership plus duplicate-link or other membership warning is
  `AVAILABLE_WITH_WARNINGS`.
- A known source aggregate cannot by itself convert link coverage into a complete count.

### 11.3 Assignee workload

The issue-count workload formula is:

```text
for each sprint:
  for each distinct (sprintId, issueKey) where issueKey is in LoadResult.issues:
    resolved assignee = first applicable:
      exactly one link-level assignee
      otherwise Issue.assignee
  group one count per distinct issueKey
```

The implementation's extra synthetic `UNASSIGNED` label is not an approved contract. The
conservative contract is:

- Exactly one link-level assignee overrides `Issue.assignee`.
- Link-level and Issue-level disagreement is preserved as `AssigneeConflict`.
- Multiple conflicting link-level assignees remain unresolved. The issue is not assigned to an
  invented value and remains excluded from the named-assignee distribution.
- Missing link and issue assignees remain unresolved. They are not assigned to an invented
  `UNASSIGNED` category unless a future project decision explicitly approves that label.
- Duplicate links do not inflate issue counts. A repeated pair contributes at most one issue.
- Unknown issue masters do not enter workload.
- Link/issue discrepancies and unresolved assignees must remain visible through conflict evidence
  and warnings.

The exact output label for missing/unresolved assignees is a project-team decision. Until it is
approved, omission plus conflict evidence is the conservative default.

### 11.4 Estimate metrics

Safely supported:

- Individual source estimate sums listed in 11.1.

Blocked:

- Combined total estimate sums across categories.
- Estimated completion or delivery values.
- Estimate-weighted assignee workload.
- Comparing `Issue.storyPoint` with link initial/current values.
- Interpreting initial versus current values as planned versus actual work.

Blocked evidence includes real source units, initial/current semantics, category overlap and
whether issue/link values correspond to sprint aggregate sums.

### 11.5 Completion and carryover

| Derived metric | Required fields | Proposed formula | Status/evidence |
|---|---|---|---|
| Completion rate | `recordedCompletedCount`, `recordedIssueCount` | `completed / issue count` candidate only | Unavailable until denominator, overlap and category semantics are approved |
| Not-completed rate | `recordedNotCompletedCount`, `recordedIssueCount` | `notCompleted / issue count` candidate only | Unavailable until denominator and overlap are approved |
| Carryover count | Historical sprint/issue membership, approved ordering/status evidence | Count of distinct issues meeting an approved carryover rule | Unavailable; `recordedNotCompletedCount` is not carryover evidence |
| Carryover rate | Approved carryover count and denominator | `carryover / approved denominator` candidate | Unavailable until historical and denominator rules are approved |
| Completed-elsewhere interpretation | `recordedCompletedElsewhereCount` and category semantics | No derived interpretation approved | Source count may be exposed; category relation to completion remains blocked |

Raw status strings in `Issue.status` and `IssueSprintLink.status` must not infer completion.
The source aggregate categories must not be combined or partitioned until overlap is established.

### 11.6 Sprint duration and scope changes

- Recorded sprint length: `Sprint.lengthDays` is an independently valid source fact.
- Date-derived duration:
  `planned = endDate - startDate`; `actual = completeDate - startDate`.
- The inclusive/exclusive convention is unresolved. The normalized model does not decide it.
- Missing either required date produces `INSUFFICIENT_DATA`, not zero.
- Scope additions: return `Sprint.recordedAddedIssueKeys` as source evidence.
- Scope-change count: possible only after confirming `issueKeysAddedDuringSprint` encoding,
  duplicates and whether the list is complete.
- Scope-change rate: blocked until an approved denominator is selected.

### 11.7 Sprint health

Future health must remain separate from:

1. **Metric availability**: whether required evidence exists.
2. **Data quality**: usability, findings, quarantine and source coverage.
3. **Sprint performance**: approved derived metrics.
4. **Health classification**: label and reasons from approved thresholds.

Candidate inputs are recorded counts, approved completion/carryover/estimate/scope metrics, and
duration. Health remains unavailable until status and category mappings, denominator rules,
evidence precedence, warning treatment and numerical thresholds are approved.

## 12. Standardized MetricResult contract

`MetricResult<T>` is the framework-independent contract for one value:

```text
{
  value: T | null,
  availability: enum,
  reasons: string[],
  sourceFields: string[],
  findingCodes: string[]
}
```

### `value`

- The source value or approved derived value.
- `null` means missing, unsupported or not safely calculable.
- A valid numeric or collection value remains its actual value.
- Zero is zero, not missing.
- An empty collection is empty coverage evidence, not `null` and not an inferred zero.

### `availability`

| State | Exact meaning |
|---|---|
| `AVAILABLE` | Required evidence exists, the formula or source-fact contract is applicable, and no relevant warning exists |
| `AVAILABLE_WITH_WARNINGS` | The value is calculable and preserved, but relevant non-blocking evidence may affect interpretation |
| `UNAVAILABLE` | Unsupported, unmapped or not approved by contract; no valid formula applies |
| `INSUFFICIENT_DATA` | Supported in principle but required evidence is absent, null or incomplete; value is null |

Rules:

- A null source field is `INSUFFICIENT_DATA`, not `UNAVAILABLE`, unless source semantics or
  mapping are also unverified.
- An unapproved formula is `UNAVAILABLE`.
- An unusable dataset makes dependent metrics `UNAVAILABLE`, not merely insufficient.
- Warning-only findings do not force `UNAVAILABLE` when the supported value remains valid.
- Relevant warnings produce `AVAILABLE_WITH_WARNINGS`.
- An unresolved conflict does not create a resolved value; it remains insufficient or unavailable
  according to the metric contract and its observations remain conflict evidence.

### `reasons`

Human-readable explanations limited to availability and interpretation. Reasons must not invent
source semantics and must distinguish empty membership, incomplete membership, null source fields
and unresolved conflicts.

### `sourceFields`

Normalized model field names used for the result. Source facts use exact `Sprint` and
`SprintEstimateSums` fields. Membership uses `IssueSprintLink.sprintId`, `issueKey` and retained
`Issue.issueKey`. Assignee workload uses link/issue assignee fields.

### `findingCodes`

Machine-readable `ValidationErrorCode` names relevant to this metric. Finding scope is preserved:

- `recordNumber == null`: dataset-level warning/finding.
- Sprint row record number: finding for that sprint only.
- Link row record number: finding for the associated `sprintId`.
- If association is impossible: preserve the finding as dataset-level evidence.

`findingCodes` identifies evidence; it does not imply that the metric value was changed silently.

## 13. Proposed framework-independent API response

The following is illustrative JSON. It describes the contract only and does not imply implementation
of endpoints or Java changes.

```json
{
  "sprintId": "S-42",
  "sourceFacts": {
    "recordedIssueCount": {
      "value": 12,
      "availability": "AVAILABLE",
      "reasons": [],
      "sourceFields": ["Sprint.recordedIssueCount"],
      "findingCodes": []
    },
    "recordedCompletedCount": {
      "value": 8,
      "availability": "AVAILABLE",
      "reasons": [],
      "sourceFields": ["Sprint.recordedCompletedCount"],
      "findingCodes": []
    },
    "recordedSprintLength": {
      "value": null,
      "availability": "INSUFFICIENT_DATA",
      "reasons": ["The source field is null"],
      "sourceFields": ["Sprint.lengthDays"],
      "findingCodes": []
    },
    "recordedAddedIssueKeys": {
      "value": [],
      "availability": "AVAILABLE",
      "reasons": [],
      "sourceFields": ["Sprint.recordedAddedIssueKeys"],
      "findingCodes": []
    }
  },
  "derivedMetrics": {
    "distinctIssueCount": {
      "value": 10,
      "availability": "AVAILABLE_WITH_WARNINGS",
      "reasons": ["Repeated sprint/issue link rows were de-duplicated"],
      "sourceFields": [
        "IssueSprintLink.sprintId",
        "IssueSprintLink.issueKey",
        "Issue.issueKey"
      ],
      "findingCodes": ["DUPLICATE_LINK_PAIR"]
    },
    "completionRate": {
      "value": null,
      "availability": "UNAVAILABLE",
      "reasons": ["Formula and category semantics are not approved"],
      "sourceFields": [
        "Sprint.recordedCompletedCount",
        "Sprint.recordedIssueCount"
      ],
      "findingCodes": []
    }
  },
  "assigneeDistribution": {
    "availability": "AVAILABLE_WITH_WARNINGS",
    "value": {
      "issueCounts": {
        "Alice": 4,
        "Bob": 3
      },
      "conflicts": [
        {
          "issueKey": "I-7",
          "resolvedAssignee": null,
          "observedAssignees": ["Alice", "Bob"]
        }
      ]
    },
    "reasons": ["Missing or conflicting assignee observations were preserved as evidence"],
    "sourceFields": [
      "IssueSprintLink.assignee",
      "Issue.assignee",
      "IssueSprintLink.issueKey"
    ],
    "findingCodes": []
  },
  "findings": [],
  "quarantinedRecords": [],
  "provenance": {
    "datasetName": "Jira Sprint Assistant dataset",
    "dataKind": "ORIGINAL_LOCAL",
    "licenceStatus": "DATA-02 status",
    "snapshotLabel": "prepared in 2020; extraction date not recorded"
  }
}
```

`null` means a missing or unavailable value. It must not be represented only by an omitted key.
The availability field is required for every metric. `issueCounts` uses a missing assignee result
only for an explicitly approved future contract; current conservative behavior leaves unresolved
conflicts out of named counts.

## 13.1 Dataset load outcome versus usability

Loading and usability are independent API concepts. `LOADED` or `PARTIALLY_LOADED` describes
whether a `LoadResult` was produced and whether records were preserved. `USABLE`,
`USABLE_WITH_WARNINGS` and `UNUSABLE` describe data interpretation. A missing or unreadable
required input must not change the usability state to apparently valid or successful loading.

The API exposes these concepts separately and preserves validation findings and quarantine
records. `dataLoaded` is true only for `LOADED` or `PARTIALLY_LOADED`; it is false for
`LOAD_FAILED` or `NOT_ATTEMPTED`.

## 14. Decision register

| ID | Category | Decision | Options | Recommended option | Evidence required | Approval status |
|---|---|---|---|---|---|---|
| D-M01 | Safe to finalize | Record source counts without recomputation | Return unchanged / derive from links | Return unchanged | Existing normalized model | Approved contract; real semantics provisional |
| D-M02 | Safe to finalize | Preserve null separately from numeric/collection zero | Null or zero or empty collection | Keep all three representations distinct | Existing normalized model and tests | Approved contract |
| D-M03 | Safe to finalize | Distinct issue identity | `(sprintId, issueKey)` / `linkId` | `(sprintId, issueKey)`; exclude unknown masters | Existing loader/model | Approved contract |
| D-M04 | Safe to finalize | Finding association | Dataset-level / source-row / link-row | Source row where association is proved; conservative dataset fallback otherwise | Finding, retained record and link/source references | Approved implementation contract |
| D-M05 | Team agreement | Assignee label for missing/unresolved cases | Omit; use `UNASSIGNED`; use `UNKNOWN`; use separate unresolved list | Omit named assignee and preserve conflicts; no invented category | Team UX and reporting decision | Pending project-team approval |
| D-M06 | Team agreement | Conflicting repeated link-level assignees | First row; last row; unresolved; explicit team rule | Keep unresolved and preserve observations | Real duplicate patterns and team rule | Pending project-team approval |
| D-M07 | Team agreement | Link-level versus issue-level conflict | Link first; issue first; unresolved | Link first only when exactly one link assignee exists | Source duplicate/precedence evidence | Recommended option; pending team approval |
| D-M08 | Real dataset profiling | Completion denominator and numerator | Recorded completed / issue count; derived status; other partition | No formula until source categories are profiled | Authorized real CSV bytes and category definitions | Blocked |
| D-M09 | Real dataset profiling | Carryover rule | Historical membership; not-completed count; explicit source field | No carryover inference until historical evidence is profiled | Sprint/issue chronology and status data | Blocked |
| D-M10 | Real dataset profiling | Punted/not-completed/completed-elsewhere overlap | Mutually exclusive; overlapping; unknown | Keep categories separate | Real sprint CSV and source documentation | Blocked |
| D-M11 | Real dataset profiling | Estimate initial/current semantics and units | Planned vs current; category timing; source-specific coding | Keep individual sums only | Real headers and unit/definition evidence | Blocked |
| D-M12 | Real dataset profiling | Scope-added key encoding | Delimited list; array; sentinel; unknown | No scope count/rate until encoding is profiled | Real `issueKeysAddedDuringSprint` values | Blocked |
| D-M13 | Real dataset profiling | Status-to-completion mapping | Explicit status set; source aggregate only; unknown | Use source aggregates only until mapping is approved | Real issue/status vocabulary | Blocked |
| D-M14 | Real dataset profiling | Planned/actual duration convention | Inclusive; exclusive; source field only | No derived duration until convention is confirmed | Real sprint date semantics and examples | Blocked |
| D-M15 | Team agreement | Numerical sprint-health thresholds | Configurable bands; fixed bands; no classification | No health classification until approved | Delivery policy and project acceptance | Pending project-team approval |
| D-M16 | Licensing/provenance | Real-source redistribution and use | Permit; deny; synthetic-only | Synthetic-only until permitted | Human/licensing grant or verified licence | Blocked by DATA-02 |

### 14.1 Safe to finalize from existing contracts

Approved for the framework-independent contract: source-fact pass-through, null/zero/empty
separation, `(sprintId, issueKey)` de-duplication, unknown issue exclusion, source-row finding
association, dataset fallback and no health/combined estimates/status derivation.

### 14.2 Require project-team agreement

Unassigned representation, conflicting link-level assignee resolution, link/issue precedence
confirmation, health threshold policy and UX evidence display.

### 14.3 Require real Spring XD profiling

Completion/carryover formulas, category overlap, estimate semantics and units, status mapping,
scope encoding and duration convention.

### 14.4 Require licensing/provenance confirmation

Use or redistribution of real Spring XD/2020 Randula-derived rows. Synthetic fixtures remain valid
for implementation behavior only.
