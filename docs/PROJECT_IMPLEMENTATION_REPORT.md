# Jira Sprint Assistant - Implementation Report

**Report date:** 18 November 2026

**Repository:** Tejesh-c-dev/jira-sprint-ai-assistant

**Project branch:** `feat/scaffold-backend-shell`

**Purpose:** Submission report for the current Sprint Health MVP implementation.

---

## 1. Executive Summary

The project has implemented a Spring Boot backend for loading, validating and analysing the
Spring XD historical sprint dataset. The application is read-only and does not use live Jira
authentication or a database.

The backend is operational with:

- real Spring XD CSV loading;
- source-fact and membership metrics;
- health diagnostics that do not invent numerical thresholds;
- historical sprint observations and comparison responses;
- versioned REST endpoints;
- OpenAPI documentation;
- structured data-quality and error responses; and
- synthetic and real-dataset test coverage.

The overall backend is substantially complete for its approved data-contract scope.
Completion, carryover, scope-rate and sprint-health formulas remain blocked until source semantics
and human approval decisions are finalized.

## 2. Technology Stack

| Area | Implementation |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build tool | Maven |
| CSV parsing | Apache Commons CSV 1.11.0 |
| REST API | Spring MVC |
| API documentation | Springdoc OpenAPI |
| Testing | JUnit 5, AssertJ, Spring MVC Test |
| Data storage | In-memory `LoadResult`; local CSV files |

No database, live Jira client, authentication system, message queue or frontend is implemented.

## 3. Repository and Data State

The real CSV files are stored locally under `data/spring-xd/`:

- `issues.csv`
- `issue_sprint_summary.csv`
- `sprints.csv`

The `/data/*` ignore rule keeps these files untracked. The README under `data/` remains tracked.

Actual loaded counts:

| Dataset | Count |
|---|---:|
| Issue master rows | 1,992 |
| Distinct retained issue keys | 1,902 |
| Issue-sprint summary rows | 2,861 |
| Sprint rows | 66 |
| Unknown issue references | 599 |
| Unknown sprint references | 0 |
| Duplicate issue rows quarantined | 90 |
| Conflicting duplicate issue keys | 85 |

The live load result is:

- Load outcome: `LOADED`
- Usability: `USABLE_WITH_WARNINGS`
- Blocking findings: 0
- Quarantined records: 90

## 4. Implemented Data Layer

### Configuration

`DataSourceProperties` provides typed configuration for:

- data directory;
- issues filename;
- issue-sprint summary filename; and
- sprints filename.

The application reads CSV files from `data/spring-xd/` by default.

### CSV ingestion

`CsvDataLoader`:

- reads UTF-8 CSVs with BOM support;
- validates required headers;
- detects duplicate headers;
- rejects malformed extra fields;
- preserves source references;
- preserves null and numeric-zero distinctions;
- preserves negative decimal source values;
- quarantines invalid records;
- reports duplicate issue/sprint masters;
- reports unknown sprint and issue references;
- reports repeated sprint/issue pairs;
- calculates file fingerprints; and
- creates a single immutable `LoadResult`.

### Domain models

Implemented normalized records include:

- `Issue`
- `Sprint`
- `IssueSprintLink`
- `SprintEstimateSums`
- `DatasetProvenance`
- `FileFingerprint`
- `SourceRef`
- `UsabilityState`
- `DataKind`

### Validation types

The data layer returns:

- `ValidationFinding`
- `QuarantinedRecord`
- `ValidationErrorCode`
- `FindingSeverity`
- `DatasetLoadOutcome`

Validation findings preserve file role, record number, field, severity, code, reason and raw value.

## 5. Implemented Metrics

### Source-fact metrics

- recorded issue count;
- recorded completed count;
- recorded not-completed count;
- recorded punted count;
- recorded completed-elsewhere count;
- recorded sprint length;
- recorded added issue keys;
- individual recorded estimate sums.

Source-fact values are returned unchanged. They are not replaced or recomputed from relationship
rows.

### Membership metrics

- distinct known issue count per sprint;
- unknown issue reference count;
- duplicate-link de-duplication by `(sprintId, issueKey)`;
- unknown issue references excluded from known-issue counts;
- no duplicate link pair counted more than once.

### Assignee metrics

- issue-count distribution by resolved link/issue assignee;
- link-level assignee precedence when a single link assignee exists;
- issue-level assignee fallback;
- conflicting assignee evidence preserved;
- unresolved assignees not given an invented identity.

Estimate-weighted assignee workload is not implemented.

### Data-quality metrics

- total findings;
- blocking findings;
- quarantined record count;
- unknown sprint reference count;
- unknown issue reference count;
- duplicate issue record count.

## 6. Metric Availability Contract

The metric contract supports:

- `AVAILABLE`
- `AVAILABLE_WITH_WARNINGS`
- `UNAVAILABLE`
- `INSUFFICIENT_DATA`
- `PROVISIONAL`
- `REQUIRES_HUMAN_DECISION`

Every `MetricResult` contains:

- value;
- availability;
- reasons;
- source fields; and
- relevant finding codes.

Null remains null, numeric zero remains zero and an empty collection remains distinct from missing
data.

## 7. Sprint Health Foundation

`SprintHealthService` evaluates diagnostic dimensions without inventing thresholds or a numerical
health score.

The overall health status is `NOT_EVALUABLE`.

Dimensions include:

1. Completion performance
2. Scope stability
3. Carryover
4. Estimate consistency
5. Workload distribution
6. Data completeness

Every dimension provides:

- status;
- explanation;
- evidence fields;
- warnings; and
- reason.

The health engine is independent of controllers and HTTP DTOs.

## 8. Historical Comparisons and Trends

The backend provides:

- sprint-by-sprint trend observations;
- source-recorded issue count observations;
- source-recorded completed-count observations;
- source-recorded punted-count observations;
- source-recorded sprint length;
- start/end dates;
- explicit sprint comparison items;
- missing-dataset limitations;
- unknown-ID omission in comparisons.

Trend ordering is:

1. start date ascending;
2. missing dates last; and
3. lexical `sprintId` for ties.

No carryover inference, percentage-change calculation, forecasting or misleading derived trend is
implemented.

## 9. REST API

### Sprint endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/sprints` | List historical sprints |
| GET | `/api/v1/sprints/{sprintId}` | Get sprint source details |
| GET | `/api/v1/sprints/{sprintId}/analysis` | Get metric and data-quality analysis |
| GET | `/api/v1/sprints/{sprintId}/health` | Get deterministic health dimensions |
| GET | `/api/v1/sprints/compare?sprintIds=...` | Compare selected sprints |
| GET | `/api/v1/sprints/trends` | Retrieve historical observations |

### Dataset endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/dataset/status` | Load outcome and usability |
| GET | `/api/v1/dataset/quality` | Relationship and quarantine quality counts |

### Error behavior

- unknown sprint: HTTP 404;
- invalid sprint ID: HTTP 400;
- missing dataset: meaningful load-failure status;
- unexpected server error: HTTP 500 with a safe generic message;
- no internal exception or stack trace is returned.

## 10. API Architecture

The API uses:

- `SprintApiController`
- `DatasetStatusController`
- `SprintReadService`
- `SprintRepository`
- `HistoricalSprintService`
- `SprintMetricsService`
- `SprintHealthService`
- explicit response DTOs;
- `ApiMapper`;
- centralized `@RestControllerAdvice`; and
- constructor injection.

Business calculations remain outside controllers.

## 11. OpenAPI

Springdoc OpenAPI provides machine-readable API metadata.

Available at:

`/v3/api-docs`

Swagger UI is not enabled.

## 12. Testing

The test suite covers:

- Spring application context;
- configuration binding and path safety;
- missing/unreadable/empty CSV files;
- BOM and real CSV loading;
- malformed and duplicate headers;
- duplicate issue and sprint records;
- unknown sprint and issue references;
- repeated link pairs;
- null and zero preservation;
- negative estimate preservation;
- metric availability states;
- finding scopes;
- unusable and warning datasets;
- assignee conflicts;
- sprint health diagnostics;
- historical ordering;
- sprint comparison;
- dataset quality;
- REST endpoint status and JSON behavior;
- OpenAPI document availability.

Latest verified total:

**70 tests, 0 failures, 0 errors, 0 skipped**

Real-data loading was also verified through live HTTP requests.

## 13. Real Dataset Investigation

The supplied real Spring XD data was profiled and loaded.

Key findings:

- 599 unknown issue references across 522 distinct keys;
- no unknown sprint references;
- 85 conflicting duplicate issue-master keys;
- 90 duplicate issue rows quarantined;
- 341 known issue keys appear across multiple sprints;
- no duplicate `(sprintId, issueKey)` pair;
- 158 summary links have differing initial/current estimates;
- one initial estimate is negative;
- no repeated membership pair is itself evidence of carryover;
- 64 issue-master keys have no summary link.

The unknown and duplicate findings are preserved as warnings, not silently discarded.

## 14. Known Limitations

### Completion

The source's completion categories do not have an approved denominator or mapping.
`Done` must not be assumed to equal issue-summary `Completed`.

### Carryover

Multi-sprint issue membership does not prove carryover.

### Scope changes

`issueKeysAddedDuringSprint` is exposed as source evidence.
Its encoding and original-scope denominator are unresolved.

### Estimates

Initial/current summary values and issue story points may have different meanings and units.
They must not be combined without evidence.

### Assignees

Issue-master assignees are display names and link assignees are usernames in many observations.
An approved crosswalk is unresolved.

### Health

Sprint health has no numerical thresholds or formula and is not classified as healthy, at-risk or
critical.

### Dataset provenance

The source repositories do not expose a clear license or reuse grant.
Real CSV files remain local and ignored. Redistribution permission remains blocked.

## 15. Remaining Work

### Backend decision work

- finalize completion numerator and denominator;
- finalize status-to-completion mapping;
- define the carryover rule;
- define scope-addition parsing and scope-change denominator;
- establish estimate units and initial/current semantics;
- establish assignee crosswalk or unresolved-assignee policy;
- approve health dimensions and thresholds.

### Frontend

- React dashboard setup;
- sprint selection UI;
- sprint summary cards;
- trend charts;
- issue and assignee tables;
- health diagnostic display;
- loading and error states;
- provenance and warning display.

### Deployment

- Jenkins build pipeline;
- Docker image;
- Kubernetes deployment;
- environment-specific dataset configuration;
- CI test execution;
- deployment documentation.

### Project documentation

- UML diagrams;
- SRS/SDS updates;
- test evidence report;
- screenshots;
- individual contribution evidence;
- Jira board and PR evidence;
- final ABL report.

## 16. Completion Estimate

The implementation is sufficiently complete for the backend data, metric and API foundation.

Indicative status:

- Backend implementation: approximately 85–90%
- React frontend: approximately 0–5%
- Jenkins/Docker/Kubernetes: approximately 0–10%
- Project report and diagrams: dependent on submission work
- Overall project: approximately 55–65%

These percentages are planning estimates, not a verified software metric.

## 17. Submission Conclusion

The submitted backend is suitable for a Java/Spring demonstration of:

- real Spring XD CSV loading;
- data-quality reporting;
- source-fact metrics;
- deterministic historical observations;
- explicit unavailable/provisional metrics;
- read-only REST APIs;
- health diagnostics without unsupported health thresholds.

The unresolved formulas and missing frontend/deployment work must be clearly identified rather
than represented as completed functionality.
