# Normalized Data Model and Data Contract — Spring XD Sprint Health MVP

> **Status: FINAL — DATA-07 DONE (2026-11-18).**
>
> This document is the decided data-layer contract for Teammate 2's sprint analytics. Its decisions
> were finalized by Teammate 1 under an explicit ticket instruction that **no teammate approval is
> required** for DATA-07; Teammate 2 review is still welcome but is not a gate (see §13.5, §14.1).
>
> It is **not** a description of the dataset: no CSV has been inspected (see
> `docs/DATA_MAPPING.md` §2.2), so **no source column name below is observed**. Every source column
> reference is a hypothesis from the upstream terminology files and stays marked **provisional**
> (§15). Questions that only real bytes can answer are listed separately in §14.2.
>
> The normalized records and provisional synthetic-only loader now exist for DATA-08 through
> DATA-11. This document remains the contract; the source mapping is still provisional.

## 0. Scope of this document

In scope: the normalized entities, their fields, identifiers, relationships, representation
rules, nullability, provenance, duplicate/unmatched handling, validation expectations, and the
exact input surface Teammate 2 may rely on.

Explicitly out of scope (other owners, other tickets):

- CSV parsing, header validation and loader code (DATA-09, DATA-10).
- Metric formulas, health bands and analysis response assembly (Teammate 2: API-01 to API-05).
- REST/OpenAPI, dashboard, CLI, CI/CD (Teammates 2–4).

## 1. Evidence labels

Same standard as `docs/DATA_MAPPING.md` §0. Nothing is promoted between labels without evidence.

| Label | Meaning |
|---|---|
| **Verified** | Confirmed this session by a command or fetched page; evidence quoted. |
| **Documented (unverified)** | Asserted upstream but never checked against the files. |
| **Proposed** | A team design decision in this document. Not a source fact. |
| **Unresolved** | No evidence exists. Must not be guessed, defaulted or filled in. |

## 2. Baseline facts this model must survive

These are the only dataset claims that carry any evidence (all **Verified** or
**Documented (unverified)**; see `docs/DATA_MAPPING.md` §1):

1. **Verified** — no CSV is present locally, and downloading it is prohibited. The model must be
   definable and testable against synthetic fixtures alone.
2. **Verified** — no licence or reuse grant exists in either upstream repository (DATA-02
   BLOCKED). Therefore model classes must never be validated by asserting real Spring XD values,
   and no document or fixture may embed original records.
3. **Documented (unverified)** — the dataset is a *static historical snapshot* prepared in 2020
   from `jira.spring.io` board 43, with "some inappropriate data removed" and commas/newlines
   stripped from strings. Consequences the model must encode:
   - absence of an issue in the data is **not** evidence that the issue never existed;
   - embedded delimiters were pre-sanitised upstream, which is a parsing risk but **not**
     permission to use `String.split(",")`;
   - the extract was filtered, so counts are not a complete project history.
4. **Documented (unverified)** — all 66 sprints are believed to be `CLOSED`. If true, no "current
   sprint" semantics exist; the model must not invent an `isCurrent` flag.
5. **Verified** — the three upstream files are byte-identical copies of the 2020 Randula dataset,
   so both repositories must appear in provenance.

## 3. Design invariants

These are the rules the model exists to guarantee. Each is a **Proposed** design commitment.

| # | Invariant | Why |
|---|---|---|
| I1 | `Issue` carries **no sprint field**. Sprint membership exists only through `IssueSprintLink`. | An issue can be linked to several sprints (carry-over, completed-in-another-sprint). A single field would silently drop history. |
| I2 | Issue-level facts and issue-in-sprint facts are separate entities. | `storyPoint` is an issue property; `initialStoryPoint`/`currentStoryPoint` are per-sprint properties. Collapsing them loses the sprint-time snapshot. |
| I3 | Sprint-level recorded aggregates are stored **as source facts**, never recomputed or back-filled by the loader. | `totalNumberOfIssues`, `completedIssuesCount` etc. are the producer's numbers. Teammate 2 may compare them to derived counts; the loader may not overwrite them. |
| I4 | Absent values stay absent. No fabricated defaults, no zero-filling, no sentinel dates. | A missing estimate is not zero; a missing date is not the epoch; a missing count is not 0 issues. (§10) |
| I5 | Every parsed record carries a `SourceRef` back to file and row. | Data-quality findings must be actionable and traceable without shipping the data. |
| I6 | `storyId` ≠ `issueKey`. `priorityId` ≠ `priority`. No assumption that either pair is interchangeable or co-coded. | Documented as distinct columns at different levels; no crosswalk exists. (§16) |
| I7 | Estimate-sum semantics are **not** decided here. All eight sprint-level sums are preserved as separate, individually-optional values. | §14.2 S1 explains why any merge would bake in an unverified meaning. |
| I8 | The model describes *what the data says*, not *what the dashboard wants*. No field exists to make a metric easy. | Prevents back-filling a denominator to avoid `INSUFFICIENT_DATA`. |
| I9 | Parsing models are separate from API response DTOs. | Teammate 2 assembles `sprint`/`metrics`/`health`/`issues`/`provenance`; the loader must not pre-shape them. |
| I10 | Every `IssueSprintLink` is a distinct record with its own `linkId`. (`sprintId`, `issueKey`) is a non-unique lookup pair and is **never** a unique key. | The source can hold several link rows for one issue in one sprint; a pair-key would silently overwrite history. (§7.1) |
| I11 | Cross-entity field precedence is fixed, not situational: link `assignee` first with `Issue.assignee` as a **marked** fallback; `Issue.summary` first with the link `summary` as fallback. | Two records describe the same facts; an unstated precedence makes the workload table and the issue table disagree. (§10.4) |
| I12 | `USABLE` / `USABLE_WITH_WARNINGS` / `UNUSABLE` describes the **input data**, never the sprint's health. | A data-quality gate and a sprint-health heuristic must not be confused in the UI, the CLI or the report. (§12.5) |

## 4. Entity overview

```text
                         ┌──────────────────────────────┐
                         │ DatasetProvenance            │  one per LoadResult
                         │  attribution, snapshot,      │
                         │  dataKind, reuse status      │
                         └──────────────────────────────┘

  ┌───────────────────────┐         ┌──────────────────────────────┐
  │ Sprint                │         │ IssueSprintLink              │
  │  sprintId  (PK)       │◄────────│  linkId      (PK, per row)   │
  │  name, state, dates   │  0..*   │  sprintId    (FK, non-unique)│
  │  recorded aggregates  │         │  issueKey    (FK, non-unique)│
  │  8 estimate sums      │         │  storyId     (opaque, NOT FK)│
  └───────────────────────┘         │  status, assignee            │
                                    │  initialStoryPoint           │
  ┌───────────────────────┐         │  currentStoryPoint           │
  │ Issue                 │◄────────└──────────────────────────────┘
  │  issueKey  (PK)       │  0..*
  │  summary, status,     │
  │  type, priority,      │
  │  storyPoint, assignee │
  └───────────────────────┘

  Every record above also carries one SourceRef.
```

| Entity | Source file (proposed, header-independent — from `DATA_MAPPING.md` §3.1) | Cardinality |
|---|---|---|
| `Sprint` | Sprint metadata CSV | 0..n sprints |
| `Issue` | Issue master CSV | 0..n issues |
| `IssueSprintLink` | Issue-summary CSV | 0..n links |
| `DatasetProvenance` | none — assembled by the loader | exactly 1 per load |
| `SourceRef` | none — embedded per record | exactly 1 per record |

Relationship rules (**Proposed**; cardinality is a hypothesis to verify, not a fact):

- `Sprint 1 ── 0..* IssueSprintLink` — a sprint with zero links is **legal**, not an error.
- `Issue 1 ── 0..* IssueSprintLink` — an issue with zero links (never planned) is **legal**.
- `Issue * ── * Sprint` via `IssueSprintLink`. Many-to-many is required, not optional.
- `IssueSprintLink` does **not** reference `storyId` as a foreign key. `storyId` is an opaque
  source value that may or may not equal an issue key. (§14.2 S2)

## 5. `Issue` — definition

Represents one row of the issue master file: a durable, project-wide issue fact. It says nothing
about which sprint(s) the issue was in.

**Responsibility:** identity, description and current-state facts for an issue, valid independent
of any sprint.

### 5.1 Core fields (surfaced to analysis/UI)

All source columns **provisional**.

| Field | Java type | Required | Source column (provisional) | Notes |
|---|---|---|---|---|
| `issueKey` | `String` | **required** | `key` | Primary identity. Opaque; trimmed; must be non-blank. Never parsed into project/number parts. |
| `summary` | `String` | optional | `summary` | Display text. May be absent; do not substitute the key. |
| `issueType` | `String` | optional | `issueType` | Raw source value. **No enum.** Casing/vocabulary unverified. |
| `status` | `String` | optional | `status` | Raw source value. **No "done"/"open" mapping** — that interpretation belongs to Teammate 2 and is currently unevidenced. |
| `storyPoint` | `BigDecimal` | optional | `storyPoint` | Issue-level points. **Not** the same quantity as any sprint `*EstimateSum` (§14.2 S1). |
| `priority` | `String` | optional | `priority` | Raw label. Unrelated to `priorityId` (§16.2). |
| `assignee` | `String` | optional | `assignee` | Display name only; no account id is documented. |
| `sourceRef` | `SourceRef` | **required** | — | Traceability (§7.3). |

### 5.2 Retained passthrough fields (not surfaced, kept verbatim)

Documented in the upstream terminology files but not needed by any MVP metric. Retained as
`Optional<String>` so that a later ticket can use them without re-profiling, and so the model
proves what it ignored rather than losing it silently.

`description`, `creator`, `reporter`, `watchCount`†, `commentCount`†, `votes`†, `fixVersions`,
`affectedVersions`, `issueLinks`, `blockedBy`, `blocks`, `dependedOnBy`, `dependedOn`, `subtasks`.

† Documented as numeric, but the type is unverified; keep the raw string until a profiled value
proves a numeric form. **Retention is not endorsement:** `blockedBy`/`blocks` are documented
yet the upstream README claims no blocked issues exist, so an empty value here must never be
read as "this issue was never blocked" (`DATA_MAPPING.md` §6).

If the real headers omit any of these, the field is simply absent from the model — the loader
must not invent it. **Unresolved:** whether all of these columns exist in the shipped files.

### 5.3 Why there is no `sprintId` on `Issue`

Invariant I1. The documented summary status vocabulary includes a "completed in another sprint"
state, and the plan requires carry-over analysis. Both are relationships, so the relationship is
its own entity. A convenience `Sprint` field on `Issue` would make the many-to-many case
unrepresentable.

## 6. `Sprint` — definition

Represents one row of the sprint metadata file, including the producer's own sprint aggregates.

**Responsibility:** sprint identity, calendar facts, and the recorded aggregate counts and estimate
sums that the analysis layer must consume as facts (I3).

All source columns **provisional**.

### 6.1 Identity and calendar

| Field | Java type | Required | Source column (provisional) | Notes |
|---|---|---|---|---|
| `sprintId` | `String` | **required** | `sprintId` | Join key. Opaque, trimmed, non-blank. Numeric-looking values stay strings (leading zeros, mixed forms unverified). |
| `name` | `String` | required-if-present | `sprintName` | Needed for the sprint picker. See §6.4 for why "required" is conditional. |
| `state` | `String` | optional | `sprintState` | Raw value. Documented as `CLOSED` for all rows; no enum until observed. |
| `startDate` | `LocalDate` | optional | `sprintStartDate` | §9 |
| `endDate` | `LocalDate` | optional | `sprintEndDate` | §9 |
| `completeDate` | `LocalDate` | optional | `sprintCompleteDate` | §9 |
| `lengthDays` | `Integer` | optional | `SprintLength` | Source-provided duration. **Do not** recompute it here from the dates. |
| `developerCount` | `Integer` | optional | `NoOfDevelopers` | **Derived by the 2020 compiler** (assignees counted per sprint), not a Jira field of record. Must be labelled derived, never "team size from Jira". |

### 6.2 Recorded aggregate counts

Stored verbatim. The loader never sums the links to fill these, and never overwrites a supplied
value (I3).

| Field | Java type | Required | Source column (provisional) |
|---|---|---|---|
| `recordedIssueCount` | `Integer` | optional | `totalNumberOfIssues` |
| `recordedCompletedCount` | `Integer` | optional | `completedIssuesCount` |
| `recordedNotCompletedCount` | `Integer` | optional | `issuesNotCompletedInCurrentSprint` |
| `recordedPuntedCount` | `Integer` | optional | `puntedIssues` |
| `recordedCompletedElsewhereCount` | `Integer` | optional | `issuesCompletedInAnotherSprint` |
| `recordedAddedIssueKeys` | `List<String>` | optional | `issueKeysAddedDuringSprint` |

**Unresolved (blocks metric correctness, not the model):**

- Whether `recordedNotCompletedCount` already includes `recordedPuntedCount`. The project plan
  already warns against double counting these; the model therefore keeps them as **two separate,
  non-additive** values and adds no `recordedTotalIncomplete` convenience field.
- Whether `recordedAddedIssueKeys` is one key, a delimited list, or a count. The documented column
  name says "keys", so it is modelled as a list, but the **delimiter and element format are
  unverified** and the type is provisional. The loader must not split on a guessed delimiter; if
  the value does not match a **confirmed** format, it stays a single-element list plus a warning.

### 6.3 Recorded estimate sums — eight distinct, individually optional values

Modelled as one nested record so the four status buckets × two magnitudes stay visibly separate:

```text
SprintEstimateSums
  completedInitial            completedCurrent
  puntedInitial               puntedCurrent
  notCompletedInitial         notCompletedCurrent
  completedElsewhereInitial   completedElsewhereCurrent
```

| Field | Java type | Required | Source column (provisional) |
|---|---|---|---|
| `completedInitial` | `BigDecimal` | optional | `completedIssuesInitialEstimateSum` |
| `completedCurrent` | `BigDecimal` | optional | `completedIssuesEstimateSum` |
| `puntedInitial` | `BigDecimal` | optional | `puntedIssuesInitialEstimateSum` |
| `puntedCurrent` | `BigDecimal` | optional | `puntedIssuesEstimateSum` |
| `notCompletedInitial` | `BigDecimal` | optional | `issuesNotCompletedInitialEstimateSum` |
| `notCompletedCurrent` | `BigDecimal` | optional | `issuesNotCompletedEstimateSum` |
| `completedElsewhereInitial` | `BigDecimal` | optional | `issuesCompletedInAnotherSprintInitialEstimateSum` |
| `completedElsewhereCurrent` | `BigDecimal` | optional | `issuesCompletedInAnotherSprintEstimateSum` |

The meaning of "Initial" versus plain is **not decided** (§14.2 S1). No derived field such as
`totalEstimate` or `scopeChange` is permitted in the data layer, because each such field encodes a
semantic choice that has no evidence behind it.

### 6.4 On "required" for `name`

`name` is required *for the MVP's sprint picker*, but its presence in the source is unverified.
The rule is therefore stated as: **if the column exists, a blank name is a warning and `name` stays
null; the record is kept.** Identity never depends on `name`. If the column proves absent, the
picker must fall back to `sprintId`, and that becomes Teammate 2/3's decision — not a fabricated
name in the loader.

## 7. `IssueSprintLink` — definition

Represents one row of the issue-summary file: the fact that an issue was recorded against a sprint,
with the sprint-time values the producer captured for it.

**Responsibility:** preserve historical membership *and* the per-sprint snapshot of status,
estimate and assignee, without collapsing either into `Issue`.

All source columns **provisional**.

| Field | Java type | Required | Source column (provisional) | Notes |
|---|---|---|---|---|
| `linkId` | `String` | **required** | — | **Record identity of this link row**, unique per link (§7.1). Loader-assigned from `SourceRef`; never a source column, never derived from `storyId`, never a (`sprintId`, `issueKey`) pair. |
| `sprintId` | `String` | **required** | `sprintId` | Join to `Sprint.sprintId`. **Non-unique** — the pair may repeat (§7.1). |
| `issueKey` | `String` | **required** | `issueKey` | Join to `Issue.issueKey`. |
| `storyId` | `String` | optional | `storyId` | **Opaque. Not a foreign key. Not assumed equal to `issueKey`.** (§14.2 S2) |
| `status` | `String` | required-if-present | `status` | Raw. Documented values include `Completed`, `Punted`, `CompletedInNotherSprint` (sic), `NotCompletedWi…` — spelling must be preserved, not corrected, and no enum until observed. |
| `assignee` | `String` | optional | `assignee` | Sprint-scoped assignee. May differ from `Issue.assignee`; both are kept (§10.4). |
| `priorityId` | `String` | optional | `priorityId` | Kept as **string** on purpose: documented numeric code, but zero-padding/coding is unverified. **Not** `Issue.priority`. (§16.2) |
| `summary` | `String` | optional | `summary` | Duplicate of `Issue.summary` in principle; kept for traceability. Precedence is unresolved (§10.4). |
| `initialStoryPoint` | `BigDecimal` | optional | `initialStoryPoint` | Sprint-time estimate for this issue. |
| `currentStoryPoint` | `BigDecimal` | optional | `currentStoryPoint` | Later/adjusted estimate for this issue. |
| `projectId` | `String` | optional | `projectID` | Passthrough only. Multi-project support is out of MVP scope; retained so filtering is possible without re-parsing. |
| `sourceRef` | `SourceRef` | **required** | — | Traceability. |

### 7.1 Link identity — **DECIDED**

Every `IssueSprintLink` is its own record with its own **unique record identity** (`linkId`),
assigned by the loader. It is not a source field and depends on no source uniqueness assumption.

Rules:

- `linkId` is stable for a given load: it is derived from the record's `SourceRef`
  (`fileRole` + 1-based `recordNumber`), so it is reproducible, traceable and collision-free.
- **(`sprintId`, `issueKey`) is never used as a unique key.** It is an ordinary, non-unique index
  pair: the same issue may legitimately appear several times in one sprint (a punted row and a
  re-added row, a status change, distinct `storyId` values). Treating the pair as a key would
  silently overwrite a source row.
- Every link source row is preserved, including exact duplicates. Nothing is collapsed,
  de-duplicated or overwritten at load time (§11.1).
- `linkId` is never derived from `storyId`, and never asserts `storyId == issueKey` (§14.2 S2).
- Sprint membership metrics count **distinct `issueKey` values**, not link rows (§11.3, §13.3).
- Duplicate handling follows §11.1.

### 7.2 What `IssueSprintLink` is *not*

- Not an audit log of every sprint transition. The snapshot is a single extract; intermediate
  moves are not recoverable.
- Not a scope-change record. Added-during-sprint information exists only as the sprint-level
  `issueKeysAddedDuringSprint` fact (§6.2). An issue present in a link is not thereby "added".
- Not a completion record. `status` is a source label of unverified meaning; it is not an
  authoritative done/not-done flag (§8, row 1 of §12).

## 8. Provenance representation

Provenance is **two-level** and never inferred from the records themselves.

### 8.1 `DatasetProvenance` — one per load

| Field | Java type | Required | Source | Notes |
|---|---|---|---|---|
| `datasetName` | `String` | required | config/provenance text | e.g. a label the team controls, not a source column. |
| `dataKind` | `DataKind` | required | loader input | `ORIGINAL_LOCAL` or `SYNTHETIC_FIXTURE`. **Mandatory** — synthetic data must never be presentable as Spring XD data. |
| `attribution` | `String` | required | `DATA_MAPPING.md` §1 | Must name both the Ayushi repository and the 2020 Randula dataset. |
| `sourceRepository` | `String` | required | as above | Primary profiled URL. |
| `licenceStatus` | `String` | required | `DATA_MAPPING.md` §1.4 | Records that DATA-02 is **blocked**; must reach the UI caveat. |
| `snapshotLabel` | `String` | required | provenance text | No extraction date exists. Must state "prepared in 2020; extraction date not recorded" rather than inventing one. |
| `knownLimitations` | `List<String>` | required | `DATA_MAPPING.md` §6 | Filtered extract, derived developer count, possibly-empty dependency columns. |
| `loadedAt` | `Instant` | required | system clock | Load time, **not** snapshot time. Must be distinguishable from it. |
| `fileFingerprints` | `List<FileFingerprint>` | required | computed | See §8.2. |

### 8.2 `FileFingerprint`

`fileRole` (`ISSUES` / `ISSUE_SPRINT_SUMMARY` / `SPRINTS`), `fileName`, `byteSize`, and
`sha256`. Purpose: prove *which* local file produced a result without committing it. A checksum
leaks no rows, so it is safe to log and display. `byteSize` and `sha256` are **Proposed**
additions; they also let the team confirm which upstream copy a local file matches.

### 8.3 `SourceRef` — one per record

| Field | Java type | Required | Notes |
|---|---|---|---|
| `fileRole` | `FileRole` | required | Which of the three inputs. |
| `recordNumber` | `int` | required | 1-based **data** row, header excluded. Fixed here as the project convention, so error messages mean the same thing everywhere. |
| `rawLineNumber` | `int` | optional | Physical file line, for humans opening the file. Only present when it can be determined safely (it cannot if records span physical lines). |

**Every parsed record carries exactly one `SourceRef`, and every quarantined record keeps the
`SourceRef` of the row it came from** (§14.1 D12). Quarantine removes a record from the resolved
collections; it never removes its traceability.

`SourceRef` deliberately carries **no raw field values**. Retaining source text per field would
turn the model into a copy of the dataset — impossible to publish, and unnecessary: validation
messages quote the offending value, which is bounded and purposeful.

## 9. Date and numeric representation

| Concern | Rule | Rationale |
|---|---|---|
| Dates | `java.time.LocalDate`. No time component, no zone. | Jira sprint dates are calendar dates; a zone would fabricate precision the source does not have. |
| Date parsing | The current synthetic-only mapping accepts ISO `yyyy-MM-dd`; the upstream format remains unverified and must not be treated as confirmed. | Guessing `dd/MM/yyyy` vs `MM/dd/yyyy` silently swaps days and months. |
| Impossible dates | Parse failure on an optional field → field is `null`, plus a **warning** carrying `fileRole`, `recordNumber`, field name and the raw value. The record is kept. | One bad date must not discard a usable sprint. |
| Timestamps, if present | Unresolved. If the source turns out to carry timestamps, either `LocalDateTime` (no zone) or a documented `Instant` conversion is agreed **before** parsing. | Silent truncation is a data loss. |
| Estimates / points | `BigDecimal`, `Optional`. Never `double`/`float`. | Points can be fractional; binary floats make totals and boundary comparisons unreliable. |
| Scale / rounding | Not normalised in the data layer. Values are preserved as parsed (trailing zeros are not significant). Rounding for display is the presentation layer's decision. | Rounding in the data layer would make `COMPLETED` sums disagree with the source. |
| Counts | `Integer`, `Optional`. | `int` cannot represent "not supplied" (I4). |
| Identifiers | `String` always — never `int`/`long`. | Leading zeros, prefixes (`XD-123`), and mixed forms are all unverified; numeric coercion destroys them. `sprintId` is also kept as a string even if it looks like a number, so joins cannot drift. |
| Booleans | **None in the model.** | Nothing in the documented schema is genuinely boolean; `status` is the tempting case and it is not safe (invariant I4). |

## 10. Null and missing-value handling

### 10.1 The vocabulary

Three states are distinguished and must never be merged:

| State | Representation | Meaning |
|---|---|---|
| Present | value | Source supplied a parseable value. |
| Missing | `null` inside `Optional`-returning accessor | Source supplied nothing, or supplied something unparseable (with a validation entry). |
| Not applicable | field absent from the model contract entirely | The source does not have this concept. |

`Optional` is used at boundaries (accessors / loader result), not as a record component type, to
keep records straightforwardly comparable in tests. The means of exposing optionality at the accessor
level is a loader implementation detail (DATA-09) that must keep the three states distinguishable.

### 10.2 Blank and sentinel rules

- Empty string and whitespace-only → **missing** (`null`). Trimming is applied to every string
  value; it is not applied to `summary`/`description` interior whitespace beyond that.
- Case-insensitive `na`, `n/a`, `null`, `none`, `-` → **not** converted to missing by default.
  These are **Unresolved**; converting them without evidence would hide real values, and a sprint
  could legitimately be named "None". Proposed interim rule: such values stay as literal text and
  raise a **warning** so the frequency can be measured, then the team decides once with evidence.
- `"0"` is a real value. `"0"` for an estimate means zero estimated work; it is not missing.
- Never treat `null` as `0` in any field, and never treat `0` as `null`. A zero denominator is a
  dataset fact that must reach `INSUFFICIENT_DATA`, not an excuse to skip the metric.

### 10.3 Missing means the metric is unavailable, not that the answer is good

If a required denominator is missing or zero, Teammate 2 must produce `INSUFFICIENT_DATA`. The data
layer's contribution is simply to leave the value `null` rather than supplying a plausible default.

### 10.4 Field precedence across entities — **DECIDED**

Two records describe the same facts at different levels. Precedence is fixed here so the issue table,
the workload table and the analysis cannot disagree:

| Fact | First choice | Fallback | Fallback marking | If both absent |
|---|---|---|---|---|
| Assignee for a sprint's issues | `IssueSprintLink.assignee` (the sprint-time fact) | `Issue.assignee` | **Required**: the row must expose that the value came from the master record, not the sprint snapshot | `null` — unknown, never rendered as "Unassigned" |
| Issue display summary | `Issue.summary` (master record) | `IssueSprintLink.summary` | Marked as sprint-snapshot text, which may be stale | `null` — never substitute `issueKey` |
| `storyId` | The link's own `storyId` | — | Never replaced by `issueKey`, never joined on | `null` |
| Priority | Link `priorityId` and Issue `priority` are **separate facts** | — | No crosswalk, no translation, no fallback between them | `null` |

Rules:

- Precedence is **ordered, not merged**: the losing value stays available verbatim and is never
  overwritten or dropped.
- A fallback is always **marked** in whatever structure the consumer builds. An unmarked fallback
  would let master-record text masquerade as a sprint snapshot.
- A mismatch between the two levels is a **reportable observation**, not an error: the master record
  can legitimately have changed after the sprint snapshot was taken.

## 11. Duplicate and unmatched-reference handling

### 11.1 Identity conflicts within a file

| Case | Handling | Severity | Rationale |
|---|---|---|---|
| Duplicate `Issue.issueKey`, rows identical after normalisation | Keep first, quarantine the rest | **Warning** | Deterministic, lossless in effect. |
| Duplicate `Issue.issueKey`, rows conflict | Keep first, quarantine the rest, do **not** merge fields | **Error** | Field-level merging would invent a record that exists in neither row. Quarantined rows stay available for inspection. |
| Duplicate `Sprint.sprintId` | Same two rules as above | Warning / Error | Sprint aggregates are facts; a conflicting duplicate makes the sprint's metrics ambiguous. |
| Repeated (`sprintId`, `issueKey`) link | Keep **all** rows; each keeps its own `linkId`; no overwrite, no merge | **Warning**, once per repeated pair, listing the `linkId`s / record numbers | Legitimate (status history, re-added work, distinct `storyId`). The pair is not a key (§7.1). Membership metrics count distinct `issueKey`s, so retaining rows cannot inflate them (§11.3). |

"Quarantine" means: excluded from the resolved collections used for analysis, but returned in the
load result with their `SourceRef` (and `linkId`, for links) and the reason. A quarantined record is
never silently dropped. Quarantine is what lets a conflicting duplicate or an unknown-sprint link be
an **error** without making the whole load `UNUSABLE` (§12.5).

### 11.2 Unmatched references

| Case | Handling | Severity | Rationale |
|---|---|---|---|
| Link references a `sprintId` absent from `Sprint` | **Quarantine** the link — kept with its `linkId` and `SourceRef`, excluded from every sprint's analysis | **Error** | The row cannot be attributed to a sprint, so no sprint's totals can legitimately include it. Keeping it in a sprint's count would corrupt a denominator. |
| Link references an `issueKey` absent from `Issue` | **Retain** the link as an unresolved link; expose `issueKey`, link fields and a data-quality marker | **Warning** | Dropping it would make the sprint's issue list and any link-derived count disagree with the recorded aggregates. The link row alone still carries status, assignee and estimates. |
| `Issue` has no links | Keep the issue | none | Perfectly normal: never planned, or filtered out upstream. |
| `Sprint` has no links | Keep the sprint | none | A sprint legitimately may have no rows (or the summary file may not cover it). Teammate 2 decides representation; the loader never fabricates issues. |
| `storyId` appears with no matching issue | Not a reference error | none | `storyId` is not a foreign key (§14.2 S2). |

### 11.3 Counting rule for repeated links — **DECIDED**

- **Sprint membership metrics count distinct `issueKey`s** among that sprint's non-quarantined
  links: an issue appearing in three link rows of one sprint contributes **1**, not 3.
- Distinctness is per sprint. An issue linked to several sprints counts once in *each* sprint's
  membership; any cross-sprint total is **not** a project-wide unique issue count and must say so.
- A retained link whose `issueKey` is absent from `Issue` still contributes its distinct key,
  because dropping it would make the sprint's derived count disagree with the recorded aggregates.
- Metrics that are genuinely per-link (for example "link rows with `status = Punted`") may count
  rows, but must be labelled **row-based** in their label and note.
- `linkId` is for identity and traceability only. It is never a metric input.

### 11.4 Reportable dataset-level conditions (not per-row)

- A sprint whose recorded aggregates disagree with link-derived counts → reported by Teammate 2, not
  the loader; the loader only guarantees both values are available.
- Repeated link rows for a (`sprintId`, `issueKey`) pair → warn once per pair, listing `linkId`s.
- All sprints having no links at all → likely the wrong file pair or a header mismatch; must be
  reported loudly, because it would otherwise surface as a uniform `INSUFFICIENT_DATA` in the UI.

## 12. Validation expectations

The loader contract (DATA-10) must produce, for every finding: `fileRole`, `recordNumber`,
`field` (nullable for row-level problems), `severity`, a human-readable `reason`, and the offending
raw value where available. Row numbers are 1-based data rows with the header excluded (§8.3).

### 12.1 Errors — the record/file cannot be interpreted safely

- Required file missing, unreadable, or empty.
- Required header missing, duplicated, or the file's column count is inconsistent with its header.
- Blank/missing required identifier (`Issue.issueKey`, `Sprint.sprintId`, `IssueSprintLink.sprintId`,
  `IssueSprintLink.issueKey`).
- Duplicate identity with conflicting values (§11.1).
- Link referencing an unknown sprint (§11.2).
- Malformed CSV structure (unreachable row boundaries, unescaped quotes).

### 12.2 Warnings — record usable, value or consistency degraded

- Optional field missing (estimate, assignee, date, summary, name, `storyId`, `priorityId`).
- Unparseable optional date or numeric value → `null` + warning.
- Duplicate identity with identical values; duplicate link pair.
- Link referencing an unknown issue.
- Spring/sprint state or link status not in the documented set (record the literal value; do not
  coerce).
- Suspicious sentinel text (§10.2).

### 12.3 Dataset-level limitations — facts the source does not contain

Not "errors"; they must be surfaced as limitations, never repaired:

- No licence/reuse grant (DATA-02 blocked).
- No extraction or snapshot date.
- Filtered extract ("some inappropriate data were removed").
- Derived `NoOfDevelopers`.
- Possibly-empty dependency columns.
- No issue history/transitions — only a single snapshot.

### 12.4 The loader's obligations

- Collect multiple findings when it is safe to continue; fail fast only when a required file or
  header is unusable.
- Expose an explicit **overall usability** state (§12.5), so a partially parsed dataset can never be
  presented as clean.
- Never compute a metric, rate or health band, and never divide by anything.
- Never mutate the input files; never read from the network; never depend on the working directory
  beyond the configured data directory.

### 12.5 Usability gate — **DECIDED**

Exactly one value per load, derived only from validation findings and required-input presence:

| State | Condition | Consequence |
|---|---|---|
| `USABLE` | No errors and no warnings. | Full analysis allowed; provenance is still shown. |
| `USABLE_WITH_WARNINGS` | At least one warning or quarantined-record error, and **no blocking error**. | Analysis allowed; warnings and quarantined records surface as data-quality notes beside the result. |
| `UNUSABLE` | A **blocking** error, or a required file / required header missing, unreadable or empty. | No sprint analysis is presented; API, CLI and UI report the unusability and its findings. |

Rules:

- The state describes the **input data**, not the sprint. It is never a health label, never coloured
  like one, and never placed where `HEALTHY`/`WATCH`/`AT_RISK`/`INSUFFICIENT_DATA` appears.
- **Blocking** vs **record-level** error: a blocking error makes the dataset unsafe to interpret as a
  whole (missing/unreadable/empty required file, missing or duplicated required header, header/column
  count inconsistency, unrecoverable CSV structure). A record-level error is one that has been
  quarantined with a reason (conflicting duplicate master record, link with unknown sprint) and
  therefore could not corrupt the resolved collections.
- Because a quarantined record is an error, `USABLE_WITH_WARNINGS` is the only state that can hold
  one. The coarse name is fixed by the project; the true severity is always available from
  `findings()`, and the analysis response must show that errors are present rather than implying a
  clean file.
- A sprint-level `INSUFFICIENT_DATA` result is unrelated: a `USABLE` dataset can still lack the
  denominator a particular metric needs.
- `UNUSABLE` is not a recovered state: the loader returns the findings and whatever partial data is
  safe, and the consumer must not present per-sprint analysis from it.

## 13. Teammate 2 integration contract

### 13.1 What Teammate 2 receives

One load result, read-only, containing:

| Component | Content | Access pattern needed |
|---|---|---|
| `Sprint` collection | resolved sprints | lookup by `sprintId`; ordered listing |
| `Issue` collection | resolved issues | lookup by `issueKey` |
| `IssueSprintLink` collection | all valid links **including** links whose issue is unresolved and every repeated (`sprintId`, `issueKey`) row, each with its own `linkId` | lookup by `linkId`; filter by `sprintId`; filter by `issueKey`; count distinct `issueKey` per sprint (§11.3) |
| quarantined records | per §11, with reason + `SourceRef` | enumerate for data-quality notes |
| validation findings | errors / warnings / limitations | enumerate, group by severity |
| `DatasetProvenance` | §8.1 | display in `provenance` block |
| usability state | §12.5 | gate the analysis response |

Proposed read-only surface (names may change in DATA-09; the **semantics** are the contract):

```text
sprints()                       -> List<Sprint>          (stable, deterministic order)
sprint(String sprintId)         -> Optional<Sprint>
issue(String issueKey)          -> Optional<Issue>
links()                         -> List<IssueSprintLink> (includes unresolved-issue links and repeated pairs)
link(String linkId)             -> Optional<IssueSprintLink>
linksForSprint(String sprintId) -> List<IssueSprintLink>
linksForIssue(String issueKey)  -> List<IssueSprintLink>
quarantined()                   -> List<QuarantinedRecord>
findings()                      -> ValidationReport
provenance()                    -> DatasetProvenance
usability()                     -> USABLE | USABLE_WITH_WARNINGS | UNUSABLE
```

Ordering promise: `sprints()` is returned in source record order, so the loader makes **no**
chronological claim. Any chronological sort is Teammate 2's, using `startDate`, and must define a
tie-break and a policy for `startDate == null`.

### 13.2 Field availability by MVP metric

Teammate 2's required metrics map onto this contract as follows. "Unavailable" means the metric
must degrade rather than guess.

| MVP metric (project plan §6.2) | Fields required | If absent/unresolved |
|---|---|---|
| Total / completed / incomplete issue counts | `recordedIssueCount`, `recordedCompletedCount`, `recordedNotCompletedCount` (source facts) | Derivation from `IssueSprintLink.status` is only legal once the status→bucket mapping is agreed (§14.2 S5); until then `INSUFFICIENT_DATA`. A derived membership count uses **distinct `issueKey`** (§11.3). |
| Completion rate | a non-null, non-zero denominator | `INSUFFICIENT_DATA`; never defaulted to 0 % or 100 %. |
| Estimate totals and estimate-based completion | §6.3 sums **and** the semantics in §14.2 S1 | **Gated (D9).** The sums may be displayed as recorded facts; no total, scope-change or estimate-derived rate may ship until S1 is verified. |
| Punted / carry-over | `recordedPuntedCount`, `recordedNotCompletedCount` | Kept separate (D10). Never added together without evidence that they are disjoint (§14.2 S4); report the two values separately meanwhile. |
| Scope additions | `recordedAddedIssueKeys` | Report as a separate metric only; never subtract from completion. Delimiter unresolved. |
| Sprint dates and length | `startDate`, `endDate`, `completeDate`, `lengthDays` | Missing `lengthDays` may be derived from dates **only** if both are present, and must be labelled derived. |
| Assignee / workload | `IssueSprintLink.assignee`, fallback `Issue.assignee` | Precedence is decided (§10.4): link value first, master value only as a **marked** fallback; a row with no assignee at either level stays unknown, never "Unassigned". |
| Historical trend | `startDate` across sprints + one agreed count metric | Sprints with `startDate == null` need a documented exclusion rule. |
| Health label | the count/estimate metrics above | `INSUFFICIENT_DATA` whenever a required denominator is missing or zero. |
| Issue table | `issueKey`, `summary`, `status`, `priority`, `storyPoint`, `assignee` | Each cell independently nullable; no placeholder values. `summary` uses `Issue.summary` first and the link `summary` as fallback (§10.4). |

### 13.3 Rules Teammate 2 must honour

1. Read only through the load result; never re-read CSV files or hard-code column names.
2. Treat recorded sprint aggregates as source facts. Comparing them with link-derived counts is
   allowed and encouraged; overwriting or silently preferring either is not — a mismatch is a
   reported data-quality note.
3. Never derive a sprint from an `Issue`; use `linksForSprint`.
4. Preserve the many-to-many case. Sprint membership metrics count **distinct `issueKey`** per sprint
   (§11.3); per-link metrics must be labelled row-based. A cross-sprint total is not a project-wide
   unique issue count and must say so.
5. Treat every `Optional` as genuinely unknown. No `orElse(ZERO)` on a denominator.
6. Keep API DTOs separate (I9); the model's field names are not the JSON field names unless
   explicitly agreed.
7. Carry `DatasetProvenance` (including `dataKind` and the blocked-licence caveat) into the analysis
   `provenance` block; a synthetic fixture must be visibly labelled in every output.
8. Do not add fields to the model to make a formula work; request a contract change instead.

### 13.4 What Teammate 2 must not assume

- That an `Issue` belongs to exactly one sprint.
- That `storyId` is an issue key, or that `priorityId` equals a priority label.
- That `initial*` and plain `*EstimateSum` are initial scope vs adjusted scope (§14.2 S1).
- That `status` is a reliable proxy for done/not-done.
- That `assignee` is a person identifier rather than a display name.
- That `state` is always `CLOSED`, or that a "current sprint" is identifiable.
- That the summary file covers every sprint, or the sprint file every link.
- That counts in the upstream filenames (1,992 / 2,861 / 67) are row counts.
- That `null` means zero, or that zero means unknown.

### 13.5 Decision authority — DATA-07 closed

The ten items in the earlier draft of this section were requests for sign-off. They are now
**decisions**, recorded with their rationale in §14.1 as D1–D12. DATA-07 was finalized by Teammate 1
under the ticket instruction that **no teammate approval is required**.

What remains true:

- Teammate 2 still owns every metric formula and may **raise a change request** against any decision
  here. A change request edits §14.1/§14.2 and is recorded in `docs/BACKEND_PROGRESS.md`; it is not a
  prerequisite for starting implementation.
- The provisional field mapping (§15) is **not** closed by this decision record. No parser, enum,
  formatter or fixture may be built from the documented column names until headers are observed.
- The estimate-semantics **gate** (§14.2 S1) is a decision to withhold estimate-derived metrics, not
  a claim about what the columns mean.

## 14. Decisions and unresolved questions

### 14.1 Decision record — DATA-07 closed (2026-11-18)

Authority: this ticket states that no teammate approval is required for DATA-07. Teammate 1 made the
following decisions. Teammate 2 may raise a change request against any of them (§13.5); a change
request edits this table rather than diverging from it silently.

| ID | Decision | Where |
|---|---|---|
| D1 | Entity set is exactly `Issue`, `Sprint`, `IssueSprintLink`, `DatasetProvenance` and `SourceRef`, with `FileFingerprint` and `SprintEstimateSums` as value objects. No field or entity is added to make a metric easy. | §4 |
| D2 | `Issue` identity is the opaque `issueKey` (`String`, trimmed, non-blank, never parsed into project/number parts). `Issue` has **no sprint field**; membership exists only through links. | I1, §5 |
| D3 | `Sprint` identity is the opaque `sprintId` (`String`, trimmed, non-blank; numeric-looking values stay strings). Identity never depends on `name`, dates or order. | §6 |
| D4 | Every `IssueSprintLink` gets its **own unique record identity** `linkId`, derived from its `SourceRef`. (`sprintId`, `issueKey`) is **never** a unique key and never a de-duplication key. | I10, §7.1 |
| D5 | Duplicate handling: **preserve every link source row**; exact duplicate master records keep the first and warn; conflicting duplicate master records keep the **first valid** record, quarantine the conflicting record and report **error**; link with unknown sprint → quarantine + error; link with unknown issue → retain + warning. | §11.1, §11.2 |
| D6 | Sprint membership metrics count **distinct `issueKey`s**; per-link metrics must be labelled row-based. | §11.3 |
| D7 | Field precedence: link `assignee` first with `Issue.assignee` as a **marked** fallback; `Issue.summary` first with the link `summary` as fallback. `storyId` stays separate from `issueKey`; `priorityId` stays separate from `priority`; no crosswalk between either pair. | I11, §10.4 |
| D8 | Types: estimates and points `BigDecimal`; counts nullable `Integer`; dates `LocalDate`; identifiers `String`; no booleans. Missing values stay `null` and are never zero-filled; `0` is a real value and is never treated as missing. | §9, §10 |
| D9 | All **eight** sprint estimate sums are preserved independently. Total scope and scope change are **not** inferred. No estimate-derived metric is enabled until the semantics in §14.2 S1 are verified. | §6.3, §14.2 S1 |
| D10 | Punted and carry-over / not-completed are **separate** facts. They are never added together without evidence that they are disjoint; the model exposes no combined field. | §6.2, §14.2 S4 |
| D11 | Usability is `USABLE` / `USABLE_WITH_WARNINGS` / `UNUSABLE` per §12.5, derived only from findings and required-input presence, and is a **data-quality gate** — never a sprint-health score and never rendered with health colours. | I12, §12.5 |
| D12 | Provenance records `fileRole` + 1-based data `recordNumber` for **every** parsed and every quarantined record, and carries the DATA-02 licensing caveat (no reuse grant; no redistribution of the original CSVs or any extract) into the UI, CLI and report. | §8, §8.3 |

### 14.2 Unresolved source-specific questions

These need real bytes (or the producer's own documentation). They cannot be settled from the upstream
README, the 2020 terminology files or a synthetic fixture. Each states the **interim rule now in
force**, so no downstream ticket is blocked waiting for the answer, plus what would close it and who
follows up. They are recorded separately from the model decisions for exactly that reason.

#### S1 — Estimate-sum semantics: `initial*` vs plain `*EstimateSum`

**Unresolved. No reading may be inferred from this document.** Two observations are recorded as *weak
evidence* for the discussion, not as a conclusion:

- The "Initial" prefix appears symmetrically on all four buckets (completed, punted, not-completed,
  completed-elsewhere). A prefix applied uniformly suggests the producer computed each bucket twice at
  two different moments, rather than the prefix meaning something bucket-specific.
- The issue-summary file documents a matching pair of per-issue fields, `initialStoryPoint` and
  `currentStoryPoint`, i.e. the same "initial vs current" idea at issue granularity.

Candidate readings, all still possible:

| # | Reading | Consequence for Teammate 2 |
|---|---|---|
| A | `initial` = estimate when the issue was planned into the sprint; plain = estimate at sprint close (adjusted total). | Scope change = Σ plain − Σ initial; completion rate should use one consistently, and the choice must be stated in the UI. |
| B | `initial` = sum restricted to issues in the sprint at start; plain = sum over all issues in that bucket regardless of when added. | Initial scope is a starting denominator; plain is the closing denominator. |
| C | plain = recomputed from `currentStoryPoint`; `initial` = from `initialStoryPoint`. | The sums are derivable from links, so they become a cross-check rather than a primary input. |

**Interim rule in force (D9):** all eight sums are loaded and exposed verbatim. No `totalEstimate`, no
`scopeChange`, no estimate-based completion percentage and no estimate-based velocity. Sprint counts
and rates use the count fields only.

**Would be closed by:** observed value patterns (does plain ≥ initial per bucket? do sums reconcile
with Σ `currentStoryPoint`?) and the producer's documentation. **Owner:** Teammate 1 profiles,
Teammate 2 chooses the formula.

Also unresolved inside S1: whether the sprint sums or the per-link points are authoritative when they
disagree, and whether `Issue.storyPoint` is in the same unit as any sprint sum.

#### S2 — `storyId` vs `issueKey`

Both are documented at summary level with unexplained distinct meanings. **Unresolved:** whether
`storyId` is a Jira numeric id, a story-point artefact id, or a duplicate of the key under another
name. **Interim rule in force (D7):** independent optional strings; never joined on, never used as a
fallback for `issueKey`, never used to build `linkId`. **Owner:** Teammate 1, DATA-03.

#### S3 — `priorityId` vs `priority`

**Unresolved:** numeric code vs display label, and whether any crosswalk exists. **Interim rule in
force (D7):** no enum, no translation table, no sorting semantics; `priorityId = 3` is never rendered
as "High" or any other label. **Owner:** Teammate 1 (DATA-05), Teammate 3 for display.

#### S4 — Do `issuesNotCompletedInCurrentSprint` and `puntedIssues` overlap?

**Unresolved:** whether punted work is already inside the not-completed count. **Interim rule in force
(D10):** two separate, non-additive fields and no combined total; a blended "incomplete or punted"
rate must not be invented. **Owner:** Teammate 2 (API-02), with Teammate 1 confirming source
behaviour.

#### S5 — Link `status` vocabulary

Documented values are inconsistent (`CompletedInNotherSprint` — likely a misspelling of
`CompletedInAnotherSprint`) and the fourth is truncated in the reference file. **Unresolved:** the
exact set, spelling and casing, and whether the labels are stable across the snapshot. **Interim rule
in force:** raw `String`, no enum, spelling preserved rather than corrected, and no done/not-done
mapping until the set is observed and Teammate 2 agrees the bucket mapping. **Owner:** Teammate 1
(DATA-05), Teammate 2 for the mapping.

#### S6 — Sentinel text and encoded fields

**Unresolved:** whether the source uses null-like sentinels (`NA`, `n/a`, `None`, `-`) and how
`issueKeysAddedDuringSprint` is encoded — one key, a delimited list, or a count. **Interim rule in
force:** sentinel-looking text stays literal and raises a warning instead of being coerced (§10.2);
the added-keys value stays a single-element list plus a warning unless a format has been confirmed;
nothing is split on a guessed delimiter. **Owner:** Teammate 1 (DATA-03).

#### S7 — Header spelling, order and presence; dates; encoding; row counts

**Unresolved.** No header has ever been observed (§15). Not determined: literal column names and
their order, whether every documented column exists, date formats, delimiter, encoding/BOM, and true
data-row counts. **Interim rule in force:** no parser, enum, formatter or fixture may be written from
the documented names; a documented column that is absent is removed from the model; an undocumented
column is recorded rather than silently added. **Owner:** Teammate 1 (DATA-03/04/05), blocked on file
availability and DATA-02.

#### S8 — Coverage and duplication of the sprint file

**Unresolved:** whether the summary file covers every sprint, whether sprint metadata can repeat a
`sprintId`, and whether `state` is always `CLOSED`. **Interim rule in force:** a sprint with zero links
and an issue with zero links are both legal and kept (no fabricated rows, no "current sprint" flag);
duplicate `sprintId` follows the master-duplicate rule (§11.1). **Owner:** Teammate 1, DATA-06.

### 14.3 Unresolved non-source items (project decisions, not data questions)

| Item | Why still open | Owner |
|---|---|---|
| Configuration defaults: normalized `issues.csv` / `issue_sprint_summary.csv` / `sprints.csv` vs the literal upstream names (spaces, embedded row counts) | A team preference, not a source fact; affects DATA-08 defaults only | Teammate 1 with the team |
| Model shape: `record` types, accessor-level `Optional`, package `com.ablmvp.sprintassistant.data.model` | Implementation style for DATA-09; does not change the semantics above | Teammate 1, confirmed at DATA-09 |
| Chronological ordering key, null-`startDate` handling, tie-break, cross-sprint trend counting | Formula and display policy; the model deliberately makes no ordering claim (`sprints()` is source order) | Teammate 2 (API-04), Teammate 3 for the chart axis |
| Health bands and every metric formula | Teammate 2's scope; this document supplies fields and data-quality gates only | Teammate 2 (API-01 to API-05) |

## 15. Field mapping status — still provisional

The source columns named in §5–§7 are **documented-but-unverified hypotheses** taken from the 2020
terminology files recorded in `docs/DATA_MAPPING.md` §3.2. They are **not** observed headers, and
finalizing DATA-07 does **not** promote a single one of them.

| Entity | Source column names | Status |
|---|---|---|
| `Sprint` | 21 documented names across identity, counts, 8 sums, `NoOfDevelopers`, `SprintLength` | **provisional — unverified** |
| `Issue` | 21 documented names incl. 14 passthrough | **provisional — unverified** |
| `IssueSprintLink` | 10 documented names (`linkId` is loader-assigned, not a source column) | **provisional — unverified** |

Consequences until headers are observed:

- The production mapping must not be treated as verified from these names. The current parser and
  fixtures use them only through the isolated `ProvisionalCsvMapping` contract, and only for
  synthetic data until real headers are authorized and observed.
- If a documented column is absent from the real header, the corresponding field is removed from the
  model; it is **not** synthesised.
- If the real header contains an undocumented column, it is not silently added to the contract — it
  is recorded, and its inclusion is a contract change logged in §14.1.
- A synthetic fixture may use these names only because they are hypotheses. The fixture proves the
  loader's behaviour, never the schema.

## 16. What this document does not establish

- It does not claim any source column exists, is spelled as written, or has the documented meaning.
- It does not define a metric, a health band, a rate, or a rounding rule.
- It does not implement, or authorise implementing, CSV parsing, controllers or APIs.
- It does not resolve DATA-02; the reuse/redistribution block is unchanged, and nothing here permits
  committing original data or any extract of it.
- It does not close the source-specific questions in §14.2; it only fixes the rule in force until they
  are answered.
