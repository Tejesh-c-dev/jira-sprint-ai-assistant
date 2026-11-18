# Duplicate Issue Analysis

Status: **EVIDENCE-BASED INVESTIGATION — 2026-11-18**

## Counts

- Distinct duplicate issue keys: 85
- Duplicate-key rows: 175
- Extra quarantined rows: 90
- Duplicate-key distribution: 80 keys occur twice and 5 keys occur three times

## Row comparison

All 85 duplicate keys have conflicting snapshots. Normalized duplicate comparison shows:

- 85 duplicate groups differ in `sprint`
- No duplicate group is an exact duplicate of every field
- No duplicate key has another field difference after the `sprint` field is excluded

Representative groups contain the same issue values while their issue-master `sprint` field
differs. This is consistent with the issue master preserving repeated issue snapshots or the
same issue associated with several sprint values.

## Loader treatment

The loader:

1. Parses each issue row.
2. Keeps the first issue master row for a key.
3. Adds a conflicting duplicate finding.
4. Quarantines later rows.

This preserves source evidence and avoids selecting a sprint snapshot by arbitrary semantics.

## Impact

The quarantine removes 90 extra issue rows, reducing:

- 1,992 parsed issue rows
- to 1,902 retained issue masters

The retained first row can affect:

- duplicate-key issue lookup
- issue-level assignee fallback
- issue-level status fallback
- issue-level story-point fallback

It does not affect summary link cardinality because 2,861 link rows and 2,861
`(sprintId, issueKey)` pairs are preserved independently.

## Safety assessment

The current policy is conservative and safe until source semantics establish that duplicate
issue rows are true historical snapshots with a canonical selection rule.

It must not:

- select a later row
- merge fields across snapshots
- silently delete snapshot evidence
- infer a `Done` mapping from duplicate status
