# Spring Health Model Readiness

Status: **DESIGN ONLY — NO HEALTH FORMULA OR THRESHOLDS**

## Completion performance

- Required fields: sprint source counts, status vocabulary or approved relationship status.
- Coverage: source counts exist for all 66 sprints.
- Limitation: source count categories sum to the source total in only 3 of 66 sprints.
- Reliability: provisional, not reliable for a single completion formula.
- Implement now: no.
- Human approval: required for numerator, denominator and category overlap.

## Scope stability

- Required fields: `issueKeysAddedDuringSprint`, original-scope evidence.
- Coverage: field is populated for all 66 sprints.
- Limitation: encoding is currently a comma-free key/count-like string; 35 distinct values.
- Reliability: partial; no proven original scope denominator.
- Implement now: source fact display only.
- Human approval: required for scope-addition parsing and rate definition.

## Carryover

- Required fields: ordered sprints, issue-sprint chronology and completion evidence.
- Coverage: 341 known issue keys appear across more than one sprint.
- Limitation: repeated membership alone does not prove carryover.
- Reliability: not reliable.
- Implement now: no carryover count/rate.
- Human approval: required for an evidence rule.

## Estimate consistency

- Required fields: issue/story and initial/current estimates.
- Coverage: initial/current link estimates are present, but one initial value is negative.
- Limitation: units and semantics are unresolved; initial and current differ for 158 link rows.
- Reliability: provisional.
- Implement now: individual source values only.
- Human approval: required for units and totals.

## Assignee workload

- Required fields: link and issue assignee fields.
- Coverage: issue masters have 2 missing assignee values; link records have 214 literal `null`
  assignees and 32 represented assignee values.
- Limitation: 2,202 link rows differ textually from issue-master assignee names; link values are
  usernames and issue values are display names. This evidence is not sufficient to infer a conflict
  for every pair because the two fields may use separate encodings.
- Reliability: source-fact workload can be counted by resolved assignee, but conflict semantics
  remain unresolved.
- Implement now: issue-count distribution with warning evidence.
- Human approval: required for missing/conflicting assignee representation.

## Data completeness

- Required fields: findings, quarantine records, source counts and relationship coverage.
- Coverage: 599 unknown references, 90 quarantined duplicate issue rows, no blocking findings.
- Reliability: reliable as a data-quality gate.
- Implement now: usability, finding and quarantine evidence.
- Human approval: not required for this gate.
