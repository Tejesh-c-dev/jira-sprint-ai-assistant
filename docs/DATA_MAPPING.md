# Data Mapping — Spring XD Sprint Health MVP

> Status: **REAL SCHEMA CONFIRMED / LICENSING BLOCKED (2026-11-18).**
> The supplied Spring XD CSV bytes have been profiled and loaded. Synthetic fixtures remain
> separate test inputs. DATA-02 remains **BLOCKED** because no reuse or redistribution permission
> has been established.

## 0. Evidence labels

- **Verified**: established from the current worktree or previously recorded source inspection.
- **Documented (unverified)**: stated by upstream documentation but not checked against CSV bytes.
- **Proposed**: an implementation or model decision, not a source fact.
- **Unresolved**: no evidence is available yet; the loader must not guess.

## 1. Verified source and reuse facts

The planned source is `Ayushi1429/Agile-Sprint-Analysis`. The recorded upstream inspection found
three CSV files and no licence file or reuse grant. The three files were also recorded as
byte-identical copies of the 2020 Randula dataset. Those are provenance facts only; they do not
grant permission to copy, transform, commit, or redistribute the data.

The user-provided local files are now present as `data/spring-xd/issues.csv` (1,992 data rows),
`data/spring-xd/issue_sprint_summary.csv` (2,861 data rows), and `data/spring-xd/sprints.csv`
(66 data rows, 67 lines including the header). Their observed headers match the real-data mapping
below. The `/data/*` ignore rule keeps these local source files out of Git; no CSV is tracked.

The upstream README's claims about approximately 1,992 issues, 2,861 summary rows and 67 sprint
file lines remain **documented, not verified row counts**. They are not used by the loader or tests.

## 2. Local input contract (DATA-08)

The loader reads three configurable paths under `app.data`:

| Property | Default |
|---|---|
| `app.data.directory` | `data/spring-xd` (configured in `application.properties`) |
| `app.data.issues-filename` | `issues.csv` |
| `app.data.issue-sprint-summary-filename` | `issue_sprint_summary.csv` |
| `app.data.sprints-filename` | `sprints.csv` |

`DataSourceProperties` resolves each file relative to the normalized configured directory and
rejects blank, absolute, or `..`-containing filenames. It does not require files at application
startup; `CsvDataLoader` reports missing or unreadable files in its `LoadResult`.

## 3. Provisional mappings used by the implementation

The following names are **hypotheses from the project terminology material**, not observed
upstream headers. They are isolated in
`src/main/java/com/ablmvp/sprintassistant/data/csv/ProvisionalCsvMapping.java` and are used by
the synthetic fixtures and loader only. They must be re-profiled and confirmed before real data
is loaded.

| Input | Normalized record | Provisional fields |
|---|---|---|
| issues file | `Issue` | `key`, summary/status/type/priority/assignee, `storyPoint`, plus documented passthrough text fields |
| issue-sprint summary file | `IssueSprintLink` | `sprintId`, `issueKey`, `storyId`, status/assignee/priority/summary, initial/current story points, `projectID` |
| sprints file | `Sprint` | `sprintId`, name/state/dates, length/developer count, recorded counts, added-issue value, and eight separate estimate sums |

The fixture date format is provisional ISO `yyyy-MM-dd`. The loader currently uses UTF-8 and
Apache Commons CSV with header-name lookup, quoted-field support and multiline-field support.
These choices describe the implementation, not verified upstream encoding, delimiter, headers or
date format.

No source metric semantics are inferred. In particular, `storyId` is not joined to `issueKey`,
`priorityId` is not translated to `priority`, status labels remain raw strings, and the eight
sprint estimate sums remain separate `BigDecimal` values. No estimate-derived metric is present.

## 4. Implemented validation and ingestion behavior (DATA-09/10)

For each load, `CsvDataLoader` returns parsed records, quarantined records, findings, provenance,
fingerprints and a data-quality usability state. Source references use a 1-based data record
number (header excluded), and link IDs are assigned per source row, so repeated sprint/issue pairs
are preserved. A quarantined link retains both its source reference and link ID.

Implemented checks include:

- missing, unreadable, empty and header-only files;
- missing or duplicate required headers;
- malformed/extra nonblank CSV fields;
- blank required identifiers, which are reported and quarantined;
- ISO date and numeric parsing; invalid optional values become `null` with a finding;
- negative numeric values, which are rejected and left `null`;
- suspicious sentinel-like text (`NA`, `N/A`, `null`, `none`, `-`) retained literally with a warning;
- exact/conflicting duplicate master records (first retained, later row quarantined);
- unknown sprint links (quarantined with an error) and unknown issue links (retained with a warning);
- repeated sprint/issue pairs (retained with one warning per pair).

Blank optional cells remain `null`; zero remains zero. The loader never fills missing values,
recomputes recorded sprint aggregates, or derives health/metric values. `USABLE_WITH_WARNINGS`
allows record-level quarantines and non-blocking findings; missing/unreadable/empty files,
required-header failures, and structural failures produce `UNUSABLE`.

## 5. DATA-11 synthetic fixture usage

`src/test/resources/fixtures/data-11/` contains three small invented CSVs and a README explicitly
stating that they are not copied, trimmed, reconstructed or derived from Spring XD. The integration
test passes their directory through `DataSourceProperties` and loads them through the actual
`CsvDataLoader`; it does not bypass parsing. It verifies quoted commas, multiline text, null
optionals, source ordering/provenance, one issue linked to multiple sprints, repeated link
preservation, unknown references, quarantine and usability.

Fixture success therefore verifies implementation behavior only. It does **not** verify upstream
headers, row counts, formats, field meanings, licensing, or the source's real data quality.

## 6. Unresolved source-specific questions

The following remain open until authorized real bytes are provided and profiled:

1. Literal headers, their order/presence, delimiter, encoding, date/number formats and actual row counts.
2. Null-like sentinels and the encoding of `issueKeysAddedDuringSprint`.
3. Whether `storyId` and `issueKey`, or `priorityId` and `priority`, share any coding.
4. The meaning of initial versus plain estimate sums and whether their units match issue/link points.
5. Status vocabulary, including the documented source spelling `CompletedInNotherSprint`.
6. Overlap between not-completed and punted counts, and whether the summary covers every sprint.
7. Duplicate identity/cardinality behavior in the real files.

Until these questions are answered, the mappings remain provisional and no production metric may
assume their semantics.

## 7. DATA-12 boundary

No upstream or derived rows are included in this repository. `.gitignore` ignores all content under
`/data/` while explicitly allowing `data/README.md`; synthetic fixtures are outside that local-data
root and are labelled as synthetic. This is a repository hygiene result, not a redistribution
permission claim. DATA-02 stays blocked pending licensing or explicit permission.
