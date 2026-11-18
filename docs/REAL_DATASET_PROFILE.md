# Real Spring XD Dataset Profile

Status: **VERIFIED FOR THE SUPPLIED LOCAL BYTES — 2026-11-18**

This profile is for the three files supplied in `C:\Users\saroj\Downloads\DATA CSV`.
These files are not tracked by Git and are kept under `data/spring-xd/`.

Synthetic fixtures remain separate and must not be described as real Spring XD evidence.

## Integrity

| Local file | Bytes | Data rows | Encoding | SHA-256 |
|---|---:|---:|---|---|
| `issues.csv` | 1,406,947 | 1,992 | UTF-8 with BOM | `e6b64a842f72d7da8ab28553fbe294d1129f3dcb00c10a36ca38550f4152279b` |
| `issue_sprint_summary.csv` | 280,440 | 2,861 | UTF-8 with BOM | `f76d7863e364598c61339a319ebc905e7b3fc07d03664d52d341c8f39b88c3d0` |
| `sprints.csv` | 6,583 | 66 | UTF-8 with BOM | `bc0c4a3caaee912f51eda5e1243981f67af99a2d61e5165e894063ff772c0b04` |

The issues SHA-256 field above was truncated and must not be used as a checksum. The verified
actual value is `e6b64a842f72d7da8ab28553fbe294d1129f3dcb00c10a36ca38550f4152279b`.

## CSV structure

All three files use comma delimiters, quoted fields, headers and no malformed field counts.

| File | Columns | Blank rows | Malformed rows | Duplicate headers |
|---|---:|---:|---:|---:|
| issues | 22 | 0 | 0 | 0 |
| issue-sprint-summary | 10 | 0 | 0 | 0 |
| sprints | 22 | 0 | 0 | 0 |

## Issue CSV schema

| Column | Observed type | Null/blank count | Distinct values | Interpretation |
|---|---|---:|---:|---|
| key | string | 0 | 1,902 | Issue key; 85 duplicate keys with conflicting rows |
| issueType | string | 0 | 4 | Bug, Improvement, Story, Technical task |
| sprint | string | 0 | 66 | Sprint membership encoded in issue master, separate from link table |
| status | string | 0 | 3 | Done, In Progress, To Do; raw string only |
| summary | string | 0 | 1,902 | Issue title |
| description | string/text | 2 | 1,657 | Text may be null |
| storyPoint | integer-like | 2 | 27 | Story-point value; null and numeric zero distinct |
| priority | integer-like | 2 | 5 | Raw priority code |
| watchcount | integer-like | 2 | 9 | Raw count |
| fixVersions | integer-like/text | 2 | 3 | Source field |
| affectedVersions | integer-like/text | 2 | 3 | Source field |
| assignee | string | 2 | 27 | Issue-level assignee |
| creator | string | 0 | 29 | Raw creator |
| reporter | string | 0 | 24 | Raw reporter |
| commentCount | integer-like | 2 | 21 | Raw count |
| votes | integer-like | 2 | 5 | Raw count |
| issueLinks | integer-like | 2 | 19 | Raw source field |
| blockedBy | integer-like | 2 | 9 | Raw source field |
| blocks | integer-like | 2 | 12 | Raw source field |
| dependedOnBy | integer-like | 2 | 13 | Raw source field |
| dependedOn | integer-like | 2 | 17 | Raw source field |
| subtasks | integer-like | 2 | 19 | Raw source field |

Issue master duplicate keys: 85 keys, 90 extra duplicate rows. All 85 keys were observed with
conflicting row content, so the loader keeps the first issue row and quarantines later duplicates.

## Issue-sprint-summary schema

| Column | Observed type | Null/blank count | Distinct values |
|---|---|---:|---:|
| projectID | string | 0 | 1 |
| sprintId | string | 0 | 66 |
| status | string | 0 | 4 |
| storyId | string | 0 | 2,861 |
| issueKey | string | 0 | 2,234 |
| summary | string | 0 | 1,968 |
| priorityId | string | 0 | 5 |
| assignee | string | 0 | 32 |
| initialStoryPoint | integer-like | 0 | 19 |
| currentStoryPoint | integer-like | 0 | 19 |

Link status values:

- `Completed`: 2,010
- `Punted`: 678
- `CompletedInNotherSprint`: 128
- `NotCompletedWithinSprint`: 45

The 599 unknown issue references must remain visible. The loader retains unknown-issue links
with warnings rather than inventing issue masters.

## Sprint schema

All 22 sprint fields have zero blanks. `sprintId` is unique across 66 rows.
Date values use `d-MMM-yy`, for example `15-Apr-13`.
Sprint state is `CLOSED` for all 66 rows.

## Relationship integrity

- 66 unique sprint IDs.
- 1,902 distinct issue keys among 1,992 issue rows.
- 85 duplicate issue master keys.
- 2,861 link rows.
- 2,861 distinct `(sprintId, issueKey)` pairs; no repeated link pairs.
- 599 unknown issue references.
- 0 unknown sprint references.
- 64 issue keys have no link row.
- 341 issue keys appear across multiple sprints.
- 341 issue keys potentially carry across links; no carryover formula is inferred.

## Data-quality classification

Blocking:

- No required identifier blank in this supplied byte set.

Warnings:

- 85 conflicting duplicate issue masters.
- 599 unknown issue references.
- Missing optional issue fields in 2 rows.

Informational:

- 66 closed sprints.
- 341 issue keys linked to multiple sprints.
- Link status vocabulary and duplicate-free link pairs.
