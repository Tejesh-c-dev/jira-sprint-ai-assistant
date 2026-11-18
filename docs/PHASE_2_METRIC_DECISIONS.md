# Phase 2 Metric Decisions

## Implemented source facts

| Metric | Formula | Status |
|---|---|---|
| Recorded issue count | Return `Sprint.recordedIssueCount` unchanged | AVAILABLE or AVAILABLE_WITH_WARNINGS |
| Recorded completed count | Return `Sprint.recordedCompletedCount` unchanged | Source fact |
| Recorded not-completed count | Return `Sprint.recordedNotCompletedCount` unchanged | Source fact |
| Recorded punted count | Return `Sprint.recordedPuntedCount` unchanged | Source fact |
| Recorded completed-elsewhere count | Return `Sprint.recordedCompletedElsewhereCount` unchanged | Source fact |
| Recorded sprint length | Return `Sprint.lengthDays` unchanged | Source fact |
| Distinct known issue count | Count distinct `issueKey` with an existing retained `Issue` master | AVAILABLE_WITH_WARNINGS when unknown or duplicate evidence exists |
| Unknown issue reference count | Count summary rows whose `issueKey` has no retained master | AVAILABLE_WITH_WARNINGS if nonzero |
| Quarantined issue record count | Count quarantined records with `ISSUES` source role | AVAILABLE_WITH_WARNINGS if nonzero |

## Explicitly unavailable or provisional metrics

| Metric | Status | Reason |
|---|---|---|
| Completion rate | REQUIRES_HUMAN_DECISION | Count categories do not consistently partition source totals |
| Carryover count/rate | UNAVAILABLE | Multi-sprint membership is not chronology |
| Scope-change rate | UNAVAILABLE | `issueKeysAddedDuringSprint` encoding and denominator unresolved |
| Combined estimate totals | UNAVAILABLE | Units and initial/current semantics unresolved |
| Estimate-weighted workload | UNAVAILABLE | No approved estimate semantics |
| Health label | NOT_EVALUABLE | No approved thresholds or weighted formula |

## Availability states

`AVAILABLE`, `AVAILABLE_WITH_WARNINGS`, `UNAVAILABLE`, `INSUFFICIENT_DATA`,
`PROVISIONAL`, and `REQUIRES_HUMAN_DECISION` are preserved independently by metric results.

## Health dimensions

Health always returns `NOT_EVALUABLE` at the overall level. Dimensions expose evidence:

- Completion performance: human decision required
- Scope stability: provisional
- Carryover: not evaluable
- Estimate consistency: provisional
- Workload distribution: available with warnings
- Data completeness: available or available with warnings
