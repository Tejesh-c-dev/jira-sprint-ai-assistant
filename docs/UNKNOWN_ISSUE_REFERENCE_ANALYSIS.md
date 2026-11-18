# Unknown Issue Reference Analysis

Status: **EVIDENCE-BASED INVESTIGATION — 2026-11-18**

## Counts

- Total unresolved link rows: **599**
- Distinct unresolved issue keys: **522**
- Unresolved rows repeated across more than one link: **77 extra rows**
- Repeated unresolved keys: **59**
- Unknown references with multiple occurrences: the most frequent key is repeated 4 times

No unresolved key can be normalized into an issue-master key by trimming or case folding.

## Distribution

Unknown references occur across many sprints rather than a single broken sprint.
The largest concentrations include:

| Sprint | Unresolved link rows |
|---|---:|
| 11 | 73 |
| 16 | 70 |
| 47 | 51 |
| 43 | 25 |
| 28 | 24 |
| 103 | 37 |
| 102 | 21 |
| 101 | 17 |

There is no known-sprint reference failure: all 599 unresolved links reference sprint IDs that
exist in the sprint master.

## Representative examples

| Sprint | Issue key | Summary | Link status | Assignee |
|---|---|---|---|---|
| 1 | XD-2 | HDFS Core writing helper classes | Punted | mark.pollack |
| 3 | XD-45 | Remove the expiry of keys in Redis based repositories | Punted | luke |
| 3 | XD-49 | Move Redis Queue Channel Adapters into spring-integration-redis | Punted | hillert |
| 5 | XD-75 | Add redis bundle to distribution zip file | Completed | iperumal |
| 8 | XD-215 | Add authentication information to twittersearch source doc | Completed | null |

## Status of unknown references

| Link status | Rows |
|---|---:|
| Punted | 347 |
| Completed | 244 |
| CompletedInNotherSprint | 5 |
| NotCompletedWithinSprint | 3 |

## Evidence for possible explanations

### No whitespace or case mismatch

Verified:

- No issue-master keys contain leading or trailing whitespace.
- No summary keys contain leading or trailing whitespace.
- Upper-case normalization resolves no additional unknown key.

### Legitimately absent issue-master rows

Possible. The issue master and summary are separate exports with 599 unresolved relationships.
The summary contains real link-level facts, but no corresponding issue master exists in
`issues.csv`.

Evidence:

- The unresolved keys are not formatting variants.
- They occur in 599 source-summary rows.
- They include both completed and punted statuses.

### Archived or filtered issue extraction

Possible. The source README says inappropriate data was removed and 64 issue-master keys have no
summary links. This supports incomplete but prefiltered exports rather than proof of deletion.

### Duplicate-master correction effect

Not sufficient. Duplicate issue keys account for issue-master quarantine, but none of the 522
unresolved keys are explained by normalization or duplicate resolution.

## Recommended treatment

- Preserve all 599 summary rows.
- Keep unknown-issue links as source records.
- Keep `UNKNOWN_ISSUE_REFERENCE` findings.
- Exclude unknown keys from distinct known-issue counts.
- Do not create guessed `Issue` objects.
- Do not use unknown references for carryover, health or assignee-master fallback.
