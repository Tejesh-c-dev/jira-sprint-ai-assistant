# Phase 2 Implementation

## Implemented

- CSV data loading and validation.
- Source-fact and membership metrics.
- Sprint health diagnostic engine.
- Deterministic historical observations.
- Sprint comparison data.
- Dataset quality counts.
- Read-only REST API.
- OpenAPI metadata.

## Sprint health

`SprintHealthService` is independent of controllers and does not apply thresholds or weighted
scoring. Its overall status is `NOT_EVALUABLE` until the project approves defensible formulas.

## Historical ordering

Historical observations are ordered by start date ascending, missing dates last, then lexical
`sprintId`. Lexical sprint IDs are not interpreted as chronology.

## Unsupported formulas

No completion rate, carryover, scope rate, combined estimates, estimate-weighted workload or
predictive trend is implemented.
