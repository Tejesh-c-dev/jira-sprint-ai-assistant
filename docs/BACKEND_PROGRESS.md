# Backend Progress Log

## Status as of 2026-11-18
Owner: Teammate 1 — Data Ingestion + Domain Model
Session state: DATA-09 IMPLEMENTED AND VERIFIED — the typed CSV configuration and CSV loader
foundation are complete; DATA-07's
normalized model, identity, duplicate,
precedence, type, estimate, usability and provenance decisions are **FINAL** in `docs/DATA_MODEL.md`
and DATA-07 is **DONE (2026-11-18)**, decided by Teammate 1 under the ticket instruction that no
teammate approval is required.
DATA-01 verified; DATA-02 BLOCKED (no reuse grant anywhere in the source chain); DATA-03/04/05/06
remain blocked because no CSV exists locally and download is prohibited, so every source column
mapping stays **provisional**. DATA-09 uses synthetic fixtures only; upstream headers remain
unverified.

## Repository baseline
- Branch: `feat/scaffold-backend-shell` (no branch switch, no reset performed)
- Working tree status before changes: one untracked file (`pom.xml`); everything else tracked; nothing staged; nothing committed in this session
- Java version: `java 25.0.4.1` / `javac 25.0.4.1` (Oracle JDK 25 LTS, `JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1`)
- Declared Java target: 21, unchanged (`<java.version>21</java.version>` → compiled as `javac --release 21`)
- Build tool and version: Apache Maven 3.9.16 (`C:\apache-maven-3.9.16-bin\apache-maven-3.9.16`)
- Spring Boot version: 3.5.6 (`spring-boot-starter-parent`)
- Project coordinates: `com.ablmvp:sprint-assistant:0.1.0-SNAPSHOT` (name `jira-sprint-assistant`)
- Existing package/root: `com.ablmvp.sprintassistant` (entry point now exists at this root)
- Existing tests/build result: `mvn -B -ntp clean test` → BUILD SUCCESS, 1 test, 0 failures (see table below)

## Existing `pom.xml` determination
The untracked `pom.xml` is the intended backend scaffold and was preserved unmodified:
- Parent `spring-boot-starter-parent` 3.5.6, `<java.version>21</java.version>`, UTF-8 source encoding
- Dependencies: `spring-boot-starter`, `spring-boot-configuration-processor` (optional),
  `org.apache.commons:commons-csv:1.11.0`, `spring-boot-starter-test` (test scope)
- Plugin: `spring-boot-maven-plugin`
No dependency, version or plugin changes were made. `commons-csv` is declared but not yet used.

## Completed in this session
- [x] Backend build baseline: compiles, tests and repackages on the installed toolchain
- [x] `.gitignore` data rules corrected and verified
- [x] `data/README.md` rewritten and made trackable
- [x] Minimal application entry point and minimal configuration added
- [x] DATA-01 — source README, attribution and file inventory verified (see "Dataset verification" below)
- [ ] DATA-02 — reuse terms: **BLOCKED**; no licence file and no reuse grant in either repository
- [ ] DATA-03 — file shapes: upstream sizes verified; headers, row counts, delimiter and encoding **not** verified
- [ ] DATA-04/05 — field mapping: **BLOCKED** pending real headers (`docs/DATA_MAPPING.md` §3)
- [ ] DATA-06 — data-quality profiling: **BLOCKED**; no records available to profile
- [x] DATA-07 — normalized model, relationships and provenance contract: **DONE (2026-11-18)**,
finalized in `docs/DATA_MODEL.md` §1–§13 and the §14.1 decision record (D1–D12); no teammate approval
required, Teammate 2 may raise a change request. Upstream field mapping remains provisional (§15)
- [x] DATA-08 — typed CSV configuration: **DONE (2026-11-18)**; implementation and focused tests pass
- [x] DATA-09 — CSV loader foundation, normalized domain records, result structure and synthetic
  fixture tests **DONE (2026-11-18)**
- [ ] DATA-10 … DATA-14 — not started

## Files created / modified
| File | Change |
|---|---|
| `.gitignore` | Replaced `data/` + `data/*.csv` + `!data/README.md` (the directory rule made the negation ineffective, so the README could never be tracked) with `/data/*` + `!/data/README.md`; removed UTF-8 BOM |
| `data/README.md` | Rewritten: local CSV placement, expected file names, tracked-README-only rule, explicit prohibition on committing originals; BOM removed |
| `src/main/java/com/ablmvp/sprintassistant/SprintAssistantApplication.java` | New minimal `@SpringBootApplication` entry point (no endpoints, no data code) |
| `src/main/resources/application.properties` | Existing application name retained; added `app.data` defaults |
| `src/test/java/com/ablmvp/sprintassistant/SprintAssistantApplicationTests.java` | New baseline `@SpringBootTest` context-load smoke test |
| `docs/BACKEND_PROGRESS.md` | This baseline record, extended with the DATA-01 to DATA-06 verification |
| `docs/DATA_MAPPING.md` | Filled in with verified source/licence facts, dataset availability and clearly separated unverified/unresolved sections; UTF-8 BOM removed |
| `docs/DATA_MODEL.md` | **DATA-07, FINAL (2026-11-18)**: decided `Issue`, `Sprint`, `IssueSprintLink`, `DatasetProvenance` and `SourceRef` contract — field-level tables, link record identity, field precedence, nullability, identifier rules, duplicate/unmatched-reference policy, usability gate, validation expectations, Teammate 2 integration contract, D1–D12 decision record and separately listed unresolved source questions (S1–S8). Upstream column names remain provisional. |

In the DATA-01 to DATA-06 pass only the two documents above were touched. No Java source,
`pom.xml`, `.gitignore`, branch, staging area or dataset file was changed.
| `pom.xml` | Inspected only — unchanged |
| `src/main/java/com/ablmvp/sprintassistant/config/DataSourceProperties.java` | Added immutable `app.data` configuration with defaults supplied by `application.properties`, resolved paths and filename safety checks |
| `src/main/java/com/ablmvp/sprintassistant/SprintAssistantApplication.java` | Added `@ConfigurationPropertiesScan` so typed configuration is registered |
| `src/main/resources/application.properties` | Added `app.data` directory and normalized CSV filename defaults |
| `src/test/java/com/ablmvp/sprintassistant/config/DataSourcePropertiesTest.java` | Added focused DATA-08 binding, default, validation, path-safety and missing-file tests |

In the DATA-07 pass only `docs/DATA_MODEL.md` (new) and this progress log were touched. No Java
source, `pom.xml`, `.gitignore`, fixture, CSV, branch, staging area or dataset file was changed.

The DATA-08 pass added only the typed configuration, its focused tests and this progress entry. No
CSV parser, loader, domain model, record validation or fixture was added. `pom.xml` was unchanged.

Nothing was committed or pushed. No existing file was deleted. Temporary CSV files used for the
ignore check were removed afterwards.

## `.gitignore` verification
Commands and observed output:

```text
$ git check-ignore -v data/issues.csv data/sprints.csv data/raw/extra.csv
.gitignore:4:/data/*    data/issues.csv
.gitignore:4:/data/*    data/sprints.csv
.gitignore:4:/data/*    data/raw/extra.csv

$ git check-ignore -v data/README.md
.gitignore:5:!/data/README.md    data/README.md
```

`git check-ignore -v` also prints the matching negation rule for the README, so the exit status
alone is not conclusive there. `git status` was used to confirm the real effect, with temporary
files created and then deleted:

```text
$ git status --porcelain -uall data
?? data/README.md

$ git status --ignored --porcelain -uall data
?? data/README.md
!! data/issues.csv
!! data/local_only/nested.csv
!! data/raw_extra.csv
```

Result: `data/README.md` is trackable (`??`, not `!!`); CSVs and other local-only dataset files at
any depth under `data/` remain ignored.

## Toolchain verification

```text
$ java -version   → java 25.0.4.1 2026-08-18 LTS
$ javac -version  → javac 25.0.4.1
$ mvn -version    → Apache Maven 3.9.16, Java version 25.0.4.1, OS Windows 11
```

## Tests actually run
| Command | Result | Evidence |
|---|---|---|
| `mvn -B -ntp clean test` | BUILD SUCCESS, exit 0 | `Compiling 1 source file with javac [debug parameters release 21]`; `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`; Spring Boot 3.5.6 context started in 0.679 s |
| `mvn -B -ntp package` | BUILD SUCCESS, exit 0 | `jar:3.4.2:jar` then `spring-boot:3.5.6:repackage`; produced `target/sprint-assistant-0.1.0-SNAPSHOT.jar` (12,025,844 bytes) |
| `git check-ignore -v`, `git status --ignored` | verified | Output quoted above |

## DATA-08 — Typed CSV configuration (DONE, 2026-11-18)

### Configuration contract

`DataSourceProperties` is an immutable record in
`com.ablmvp.sprintassistant.config`, registered through `@ConfigurationPropertiesScan`, with the
prefix `app.data`:

| Property | Default | Environment variable |
|---|---|---|
| `app.data.directory` | `data` | `APP_DATA_DIRECTORY` |
| `app.data.issues-filename` | `issues.csv` | `APP_DATA_ISSUES_FILENAME` |
| `app.data.issue-sprint-summary-filename` | `issue_sprint_summary.csv` | `APP_DATA_ISSUE_SPRINT_SUMMARY_FILENAME` |
| `app.data.sprints-filename` | `sprints.csv` | `APP_DATA_SPRINTS_FILENAME` |

The API exposes `dataDirectory()`, `issuesPath()`, `issueSprintSummaryPath()` and `sprintsPath()`.
Each resolved file path is relative to the normalized configured directory. The three filenames must
be non-blank and relative, and any filename containing a `..` path segment is rejected. The data
directory itself may be relative or absolute. No file existence check occurs during configuration
binding, so a missing dataset does not prevent startup; the future loader owns that check.

### Verification

`mvn -B -ntp test` → **BUILD SUCCESS**, exit 0. The run reported **8 tests, 0 failures, 0 errors,
0 skipped**: 7 focused DATA-08 tests and the existing Spring context smoke test. The focused tests
cover defaults, custom directory and filenames, environment-variable binding, blank filename
rejection, traversal rejection, absolute filename rejection and initialization with missing CSVs.

## DATA-09 — CSV loader foundation (DONE, 2026-11-18)

Implemented the finalized DATA-07 normalized records under `com.ablmvp.sprintassistant.domain`:
`Issue`, `Sprint`, `IssueSprintLink`, `SprintEstimateSums`, `DatasetProvenance`, `FileFingerprint`,
`SourceRef`, and the supporting enums. Nullable source facts remain `null`; identifiers remain
strings, estimates use `BigDecimal`, counts use nullable `Integer`, and dates use `LocalDate`.
Every link has a loader-assigned, source-row-derived `linkId`; repeated issue/sprint pairs are
preserved.

`CsvDataLoader` in `com.ablmvp.sprintassistant.data.csv` is the loader entry point. It resolves the
three configured DATA-08 paths and has separate parsing paths for issues, issue-sprint summaries,
and sprints. It uses UTF-8 Commons CSV parsing, header-name lookup, quoted/multiline field support,
source-order preservation, 1-based data record numbers, and SHA-256 file fingerprints. No network,
controller, API, database, metric, or health code was added.

The isolated mapping contract is
`src/main/java/com/ablmvp/sprintassistant/data/csv/ProvisionalCsvMapping.java`. Its explicit
synthetic headers and `yyyy-MM-dd` date fixture format are **provisional only**; no upstream header
or date format has been verified. It is intended to be replaced after real headers become available.

`LoadResult` carries parsed issues, sprints, links, quarantine records, validation findings,
provenance, and input `UsabilityState`. DATA-09 provides only basic parse findings and successful
loading; detailed DATA-10 validation policies remain outstanding.

Tests are in `CsvDataLoaderTest` and use temporary synthetic CSVs. `mvn -B -ntp test` passed:
10 tests, 0 failures, 0 errors, 0 skipped. Coverage includes header mapping, quoted commas,
escaped quotes, multiline fields, source ordering/numbering, repeated link pairs, null optional
values, BigDecimal and LocalDate parsing, role assignment, and provenance fingerprints.

Explicit handoff note: upstream mappings remain unverified because original CSVs are unavailable
and must not be downloaded or committed.

## Java compatibility findings
- JDK 25 compiles the project at `--release 21` without errors or warnings; the declared Java 21
  target is unchanged and Spring Boot 3.5.6 was **not** upgraded.
- The Spring Boot 3.5.6 context loads successfully on JDK 25 (`contextLoads` passes).
- Non-fatal JDK 25 warnings appeared during tests; none fail the build:
  - `Mockito is currently self-attaching to enable the inline-mock-maker … will no longer work in future releases of the JDK`
  - `WARNING: A Java agent has been loaded dynamically (byte-buddy-agent-1.17.7.jar)`
  - Hints suggesting `-XX:+EnableDynamicAgentLoading` / `-Djdk.instrument.traceUsage`
- Consequence for later work: loader tests that use Mockito will rely on the same self-attach
  mechanism. It works today; if CI or a future JDK blocks it, add Mockito as an explicit
  `-javaagent` in surefire `argLine` rather than changing the Java target.
- The starter is `spring-boot-starter` (not `-web`), which is correct for the data layer; Teammate 2
  adds the web starter with the REST scope.

## Decisions and assumptions
- Preserved the untracked `pom.xml` as the team scaffold: it matches the agreed stack (Java 21,
  Spring Boot 3.5.6, Commons CSV 1.11.0) and its description names Teammate 1's data layer.
- Package root fixed as `com.ablmvp.sprintassistant`, matching the documented plan.
- Added only a shell: entry point, `application.properties` and one context-load test. No domain
  models, loader, validation, metrics, controllers, frontend or CLI were created.
- `.gitignore` now ignores every file under `data/` except the README, rather than naming CSVs, so
  nested folders and other local dataset artefacts also stay untracked.
- `data/README.md` lists the file names as the current expectation; DATA-08 must make directory and
  file names configurable via `@ConfigurationProperties` instead of hard-coding them.

## Dataset verification — DATA-01 to DATA-06 (2026-11-18)

Scope: source verification and dataset availability only. No loader, model, validation, metric, API
or UI code was written; no CSV was downloaded, staged or committed; no branch change.

### Status
| Ticket | Requirement | Status | Evidence summary |
|---|---|---|---|
| DATA-01 | Source README, attribution and snapshot context | **DONE (verified)** | Upstream README and complete tree fetched; attribution recorded in `docs/DATA_MAPPING.md` §1 |
| DATA-02 | Reuse/redistribution permission check | **BLOCKED** | No licence file in either repository; READMEs grant nothing; see matrix below |
| DATA-03 | File sizes, headers, row counts and formats | **PARTIAL — BLOCKED** | Sizes verified from upstream Git metadata; headers/row counts/delimiter/encoding not observable (0 of 3 files present locally) |
| DATA-04 | Sprint field mapping | **BLOCKED** | Depends on DATA-03; no header observed, so no mapping written |
| DATA-05 | Issue and issue-to-sprint mapping | **BLOCKED** | Depends on DATA-03; no header observed, so no mapping written |
| DATA-06 | Data-quality profiling | **BLOCKED** | No records available; nulls, duplicates and relationships all unmeasured |

### Source and attribution (verified)
- Profiled repository: `https://github.com/Ayushi1429/Agile-Sprint-Analysis`, default branch `main`,
  single branch, no others.
- Inspected commit: `17515a236ecf0d82510bcf875b6453094e3cf057` (`git ls-remote --symref … HEAD`).
- Repository created 2025-06-25, last push 2025-06-27, 0 stars, 0 forks, no description.
- Complete upstream tree is **five files**, listed with byte sizes in `docs/DATA_MAPPING.md` §1.1:
  one notebook (971,334 B), `README.md` (2,103 B), and the three CSVs
  (1,406,947 B / 280,440 B / 6,583 B).
- **No licence file exists**: `LICENSE`, `LICENSE.md` and `LICENSE.txt` all return 404 on `main`, and
  the authoritative full tree listing contains no licence of any name and no `docs/` folder.
- Upstream README describes "real-world Agile sprint data from the Spring XD open-source project"
  over "66 sprints" and names the source only as "Public Agile Jira Dataset (Spring XD)". It gives
  no producer, no extraction method, no snapshot date and no reuse terms.

### Provenance chain (verified)
The three CSVs are byte-identical to files in `RandulaKoralage/AgileScrumSprintVelocityDataSet`
(branch `master`), folder `Agile Scrum Dataset/Finalized Datasets for Spring XD Project by Randula/`:
identical sizes **and** identical Git blob SHAs (`7101b1ef…`, `28fc6912…`, `008f4f6e…`). That is the
same content, not a re-export. That repository's README states the data was prepared for a 2020
final-year project from `jira.spring.io` board 43 via the JIRA REST and Greenhopper APIs, that
"New lines, tabs and commas were removed from strings", that team size is derived by counting
assignees per sprint, and that "Some inappropriate data were removed". It also ships **no licence
file** (24 files inspected). The Ayushi copy does not credit that compiler.

### DATA-02 decision (BLOCKED) and evidence
| Use | Permitted? |
|---|---|
| Personal/local analysis | Not explicitly permitted — no licence, no terms; silence is not permission |
| Modification | Not explicitly permitted |
| Redistribution of the original CSVs | **Not permitted** — no licence in either repository |
| Redistribution of derived/transformed data | **Not permitted** — no derivative grant |
| Attribution condition | None stated (no licence exists); crediting both repos remains required by this project's own docs |

Consequence: no original CSV, and no derived extract or trimmed sample of one, may be committed,
staged, pushed or redistributed. Only clearly labelled synthetic fixtures may be committed. Local
classroom/viva use of the real files is a human decision, not a licence right.

### Dataset availability (verified)
`data/` contains only `README.md` (1,380 B). A worktree-wide `*.csv` search (excluding `target/` and
`.git/`) returned nothing. All three expected inputs are missing:

| Expected path | Upstream file | Status |
|---|---|---|
| `data/issues.csv` | `Spring XD Issues 1992.csv` (1,406,947 B) | **MISSING** |
| `data/issue_sprint_summary.csv` | `Spring XD Issues Summary 2861.csv` (280,440 B) | **MISSING** |
| `data/sprints.csv` | `Spring XD Sprints 67.csv` (6,583 B) | **MISSING** |

### Profiling results
**No CSV was profiled.** Not determined for any file: literal headers, row count excluding header,
delimiter, encoding, date/number formats, null/blank counts, duplicate identifiers, invalid values,
and issue/summary/sprint relationships. The 1,992 / 2,861 / 67 figures come from filenames and the
upstream README only; they are claims and must never be logged as observed row counts.
`docs/DATA_MAPPING.md` therefore labels §2.2, §3, §4, §5 and §6 as blocked/unresolved, and puts the
2020 terminology files in §3.2 explicitly as **unverified reference only**.

### What is needed to continue
1. A human ruling on DATA-02 (permission, or an explicit decision that local-only classroom use is
   acceptable and that nothing derived will be published).
2. The three files placed manually at the paths above (or the literal upstream names, if the team
   chooses those) — the loader must not download them.
3. A team decision on file naming for the DATA-08 defaults, since the upstream names contain spaces
   and row-count claims while `data/README.md` expects normalized names.
4. If reuse cannot be cleared: proceed with synthetic-only fixtures, keep the mapping provisional,
   and never describe synthetic rows as real Spring XD records.

## DATA-07 — Normalized domain model and data contract (DONE, 2026-11-18)

Scope: documentation of the decided data contract only. No record class, loader, validation,
metric, controller, API, frontend or CLI code was written. No CSV was downloaded, read, staged or
committed. No branch change, no commit, no push.

### Status

| Ticket | Requirement | Status |
|---|---|---|
| DATA-07 | Finalize normalized model and provenance decisions | **DONE (2026-11-18)** |

Finalized in `docs/DATA_MODEL.md`, with D1–D12 in its §14.1. The ticket states that no teammate
approval is required, so the former "agree with Teammate 2" gate was replaced by a decision record.
Teammate 2 review is still welcome and may raise a change request; it no longer blocks the ticket.

### What was decided

- **Entities:** exactly `Issue`, `Sprint`, `IssueSprintLink`, `DatasetProvenance` and `SourceRef`,
  with `FileFingerprint` and `SprintEstimateSums` as value objects.
- **Identity:** `Issue` → opaque `issueKey`; `Sprint` → opaque `sprintId`; every `IssueSprintLink`
  gets its own unique record identity `linkId` derived from its `SourceRef`. (`sprintId`, `issueKey`)
  is **never** a unique key and never a de-duplication key.
- **Sprint membership:** `Issue` has no sprint field; membership exists only via `IssueSprintLink`,
  which is many-to-many and preserves an issue appearing in several sprints.
- **Duplicates/mismatches:** every link source row preserved; exact duplicate master records → keep
  first + warning; conflicting duplicates → keep first valid, quarantine the conflicting record +
  error; unknown sprint → quarantine link + error; unknown issue → retain link + warning.
- **Counting:** sprint membership metrics count **distinct `issueKey`s**; per-link metrics must be
  labelled row-based.
- **Precedence:** link `assignee` first with `Issue.assignee` as a **marked** fallback; `Issue.summary`
  first with the link `summary` as fallback. `storyId` ≠ `issueKey` and `priorityId` ≠ `priority`,
  with no crosswalk.
- **Types:** `String` identifiers (never numeric), `BigDecimal` estimates (never `double`), nullable
  `Integer` counts, `LocalDate` dates, **no booleans**; absent values stay `null`, never zero-filled.
- **Estimate semantics:** the eight documented sprint estimate sums
  (`completedIssuesInitialEstimateSum`/`completedIssuesEstimateSum`, and the punted, not-completed and
  completed-elsewhere pairs) stay **eight separate optional `BigDecimal`** values. No inferred total
  scope, no `scopeChange` field, and **no estimate-derived metric** until the semantics are verified.
- **Punted vs carry-over:** kept as separate, non-additive facts; never summed without evidence that
  they are disjoint.
- **Usability gate:** `USABLE` (no errors, no warnings), `USABLE_WITH_WARNINGS` (warnings or
  quarantined-record errors, no blocking error), `UNUSABLE` (blocking error, missing required file or
  header). This is a data-quality gate, **not** a sprint-health score.
- **Provenance:** `fileRole` + 1-based data `recordNumber` on every parsed and every quarantined
  record; the DATA-02 licensing caveat (no reuse grant; no redistribution of the originals or any
  extract) must reach the UI, CLI and report.

### Source-fact separation

The document does not present any source column as observed. All column names in its field tables are
marked **provisional**, taken from the documented-but-unverified terminology files already recorded in
`docs/DATA_MAPPING.md` §3.2. Finalizing DATA-07 did **not** promote any mapping to verified: DATA-03 to
DATA-06 remain BLOCKED, so the model is decided while the field mapping inside it is not. Design
decisions are labelled **DECIDED** with a D-id; anything that needs real bytes is labelled
**Unresolved** and listed under §14.2 (S1–S8).

### Decisions closed in this pass

The previously listed open items are now resolved for implementation purposes: link record identity
(per-row `linkId`, never the pair), duplicate and unmatched-reference policy, assignee and summary
precedence, the usability state definition, and the distinct-`issueKey` counting rule. The estimate
semantics were **closed as a gate** instead of a guess: all eight sums pass through untouched and no
estimate-derived metric may ship.

### Unresolved source-specific questions carried forward (S1–S8)

Recorded separately in `docs/DATA_MODEL.md` §14.2, each with the rule in force until it is answered:

1. **S1** — `initial*` vs plain `*EstimateSum` meaning; also whether sprint sums or per-link points win
   when they disagree, and whether `Issue.storyPoint` shares their unit.
2. **S2** — `storyId` vs `issueKey` (independent opaque strings; never joined on).
3. **S3** — `priorityId` vs `priority` (no crosswalk, no enum, no display translation).
4. **S4** — overlap between `issuesNotCompletedInCurrentSprint` and `puntedIssues` (kept separate).
5. **S5** — link `status` vocabulary and spelling (raw strings; the documented
   `CompletedInNotherSprint` misspelling must be preserved, not corrected).
6. **S6** — null-like sentinels and how `issueKeysAddedDuringSprint` is encoded.
7. **S7** — header names/order/presence, date formats, encoding, and true row counts.
8. **S8** — whether the summary file covers every sprint, duplicate sprint metadata, and whether
   `state` is always `CLOSED`.

Non-source items still open (project decisions, not data questions) are tracked in §14.3: DATA-08 file
naming, DATA-09 model shape, ordering/trend policy and all metric formulas (Teammate 2).

### Verification

Documentation-only change, so no compilation or test run was required and none was performed
(no Java source, `pom.xml` or fixture was touched). The tree was inspected to confirm this:

```text
$ git status --porcelain -uall
exit=0
 M .gitignore                                          (pre-existing, untouched this pass)
 M docs/BACKEND_PROGRESS.md                            (this pass)
 M docs/DATA_MAPPING.md                                (pre-existing, untouched this pass)
?? data/README.md                                      (pre-existing, untouched this pass)
?? docs/DATA_MODEL.md                                  (new in the DATA-07 pass, edited this pass)
?? pom.xml                                             (pre-existing, untouched this pass)
?? src/main/java/com/ablmvp/sprintassistant/SprintAssistantApplication.java   (pre-existing)
?? src/main/resources/application.properties                                   (pre-existing)
?? src/test/java/com/ablmvp/sprintassistant/SprintAssistantApplicationTests.java (pre-existing)

$ ls data/
README.md            # no CSV present — unchanged: the dataset is still unavailable

$ wc -l docs/DATA_MODEL.md
787 docs/DATA_MODEL.md

$ grep -n 'PROPOSED\|NOT AGREED\|Confirmation required' docs/DATA_MODEL.md
exit=1                # no stale pre-decision framing left in the document
```

No file was staged, committed, pushed or reverted; no branch was changed. Other entries in the
listing belong to earlier passes and were not modified.

### Decision record

| Item | Status |
|---|---|
| Teammate 1 (decision authority) | Decided 2026-11-18 under the ticket's no-approval-required instruction |
| Teammate 2 (consumer review) | Optional — a change request updates `docs/DATA_MODEL.md` §14.1 rather than blocking the ticket |
| DATA-07 marked DONE | **Yes** — 2026-11-18 |
| Unresolved source-specific questions | Listed separately in `docs/DATA_MODEL.md` §14.2 (S1–S8) |

## Known issues / open questions
- **Dataset reuse terms unconfirmed (DATA-02 is BLOCKED).** No licence exists in the profiled repo or
  in the 2020 repository it copies; do not commit or redistribute the CSVs or any extract of them.
- **DATA-07 was closed without teammate sign-off**, by explicit ticket instruction. The decisions are
  documented and reversible through a recorded change request, but no second person has reviewed the
  contract yet; a review is still advisable before DATA-09 hardens it into Java types.
- **No original CSVs available locally.** `data/` contains only `README.md`, so headers, row counts and
  formats (DATA-03) cannot be profiled yet; the mapping stays provisional and synthetic fixtures must
  be used.
- **Attribution is incomplete at source.** The dataset's real compiler (2020, `jira.spring.io` board 43)
  is not credited by the profiled repository; the report should cite both repositories.
- JDK 25 is installed system-wide; Teammate 4's Jenkins/Docker images should pin JDK 21 so the runtime
  matches the declared target even though compilation succeeds on 25.
- No CI pipeline and no `mvnw` wrapper exist yet (Teammate 4 scope); builds need a local Maven 3.9.16.
- The original files carried UTF-8 BOMs / mixed CRLF; `.gitignore` and `data/README.md` are now clean
  LF with no BOM.

## Not started / explicitly out of scope for Teammate 1
- Production metrics and health bands (Teammate 2: API-01 to API-05)
- REST endpoints and OpenAPI/API tests (Teammate 2: API-06 to API-14)
- Dashboard and CLI (Teammate 3)
- Jenkins, Docker, Kubernetes and ABL report ownership (Teammate 4)

## Handoff to Teammate 2
Baseline: application shell at `com.ablmvp.sprintassistant`; no data-layer code exists yet. Build with
`mvn -B -ntp test` (exit 0 today).

Contract: `docs/DATA_MODEL.md` (DATA-07, **FINAL / DONE**). It defines the entity set (`Issue`,
`Sprint`, `IssueSprintLink`, `DatasetProvenance`, `SourceRef`), field-level nullability, identifier and
relationship rules, the per-row `linkId` identity, duplicate/unmatched-reference policy, field
precedence, the usability gate, validation expectations, the read-only access surface and the
metric→field availability table. Decisions D1–D12 are binding for implementation; the source-specific
questions S1–S8 list the rule in force where evidence is missing.

Two hard gates for Teammate 2:

1. **No estimate-derived metric** until `initial*` semantics are verified (§14.2 S1); the eight sums may
   be displayed as recorded facts only.
2. **Sprint membership counts distinct `issueKey`s**, not link rows; subtract for nothing, and never
   treat the pair (`sprintId`, `issueKey`) as unique.

The `LoadResult` shape, configuration properties and real field mapping arrive with DATA-03 to DATA-13.

## Final DATA-07 through DATA-13 audit (2026-11-18)

This section is the current status summary; earlier sections preserve historical handoff notes.

| Ticket | Current status | Evidence |
|---|---|---|
| DATA-07 | **DONE** | Domain contract in `docs/DATA_MODEL.md`; implementation checked against it. |
| DATA-08 | **DONE** | `DataSourceProperties` supplies the three configurable paths and path-safety tests pass. |
| DATA-09 | **DONE** | Domain records, Commons CSV loader, result and provenance types are implemented. |
| DATA-10 | **DONE** | Validation, findings, quarantine and usability behavior are implemented and tested. |
| DATA-11 | **DONE** | Three invented fixtures exercise `CsvDataLoader` end to end. |
| DATA-12 | **DONE** | No original/derived rows are present; local data ignore rules were audited. |
| DATA-13 | **DONE** | `docs/DATA_MAPPING.md` now describes provisional mappings, verified boundaries, behavior and unresolved questions. |
| DATA-14 | **PENDING** | Out of scope for this audit. |

DATA-02 remains **BLOCKED**: no licensing or reuse/redistribution permission has been established.
No upstream CSVs were downloaded, and no metrics, sprint-health calculations, APIs, controllers,
frontend, CLI, database, CI/CD, Docker or Kubernetes work was added.

Verification command: `mvn -B -ntp clean test` — **BUILD SUCCESS**, 24 tests, 0 failures, 0 errors,
0 skipped.

The implementation audit confirmed that configured paths are used by the loader, records retain
source references, missing values stay null, multiple sprint links remain distinct, unknown-sprint
links are quarantined with link identity, unknown-issue links are retained with warnings, and no
unsupported metric assumptions are made. Negative numeric values, blank required-identifier rows,
and quarantined-link identity were corrected in this final pass; invalid rows remain absent from
resolved data and retain a finding/quarantine trail.

## PR and review
No branch, commit, push or PR was created in this session, per task restrictions.

## DATA-10 — Structured validation (DONE, 2026-11-18)

Implemented in `CsvDataLoader`, `ValidationFinding`, `ValidationErrorCode` and `QuarantinedRecord`.
Findings carry file role, 1-based data record number when available, field when available, severity,
stable error code, safe human-readable reason, and a field-level raw value where useful. Messages never
include complete CSV rows.

Implemented rules: missing/unreadable/empty/header-only files; missing and duplicate headers; extra
nonblank CSV fields; blank required identifiers; provisional ISO dates; numeric parsing and prohibited
negative values; literal unknown sentinel warnings; exact/conflicting Issue and Sprint duplicates;
unknown sprint quarantine; unknown issue retention with warning; repeated link retention with one warning
per pair; and source references on findings and quarantined records. Null and zero remain distinct.

Quarantined records are excluded from resolved collections but returned in `LoadResult`. Exact duplicate
masters keep the first record and quarantine later copies with a warning. Conflicting masters keep the
first and quarantine later copies with an error. Unknown-sprint links are quarantined with an error;
unknown-issue links remain available with a warning. Link rows are never deduplicated.

Usability is `USABLE` with no findings, `USABLE_WITH_WARNINGS` for non-blocking findings, and
`UNUSABLE` for blocking file/header/structure findings. This remains separate from sprint health.

Verification: `mvn -B -ntp clean test` — BUILD SUCCESS; 22 tests, 0 failures, 0 errors, 0 skipped
(10 existing/config/application tests and 12 DATA-10 validation tests). Synthetic fixtures only.

Remaining limitation: all provisional headers, date format, sentinel meanings, and source-specific
field mappings remain unverified because the original CSVs are unavailable and must not be downloaded.
DATA-11 must resolve upstream profiling/mapping when authorized data is available; metrics, health,
controllers, APIs, frontend and CLI remain out of scope.

## DATA-11 — Synthetic fixtures and end-to-end ingestion tests (DONE, 2026-11-18)

DATA-11 is complete using synthetic-only data. Three small deterministic fixtures were added under
`src/test/resources/fixtures/data-11/`: `issues.csv`, `issue_sprint_summary.csv`, and `sprints.csv`,
with a README explicitly stating that every row is invented and is not original Spring XD data.
No upstream CSV was downloaded, copied, reconstructed, trimmed, or derived.

The integration test `CsvDataLoaderFixtureIntegrationTest` loads all three files through configured
`DataSourceProperties` and `CsvDataLoader`. It verifies quoted commas, multiline text, omitted
optional values, record ordering and source references, multiple sprint links for one issue,
duplicate-link preservation and finding, unknown-issue retention and warning, unknown-sprint
quarantine and error, all three provenance fingerprints, the DATA-02 provenance caveat, and
`USABLE_WITH_WARNINGS` usability.

Verification: `mvn -B -ntp clean test` — BUILD SUCCESS with 23 tests, 0 failures, 0 errors, and
0 skipped (the prior 22 tests plus this integration test).

Remaining blockers are unchanged: DATA-02 has no confirmed reuse grant; DATA-03/04/05/06 source
profiling and mapping remain blocked because the original CSVs are unavailable and must not be
downloaded. The fixture headers remain the provisional DATA-08 mapping. Metrics, health calculations,
APIs, controllers, frontend, CLI, database, CI/CD, Docker, and Kubernetes remain out of scope.

## Suggested next action
DATA-10 is complete. DATA-11 should address upstream profiling and mapping when permitted, while keeping the source mapping
provisional. The DATA-02 ruling and the three CSVs are still required before DATA-03 to DATA-06 can
move, and the unresolved source questions S1–S8 stay open until then.

Teammate 2 may review `docs/DATA_MODEL.md` at any time; a change request updates its §14.1 and this
log. No confirmation is needed to start consuming the contract, and no estimate-derived metric may be
built until S1 is closed.
