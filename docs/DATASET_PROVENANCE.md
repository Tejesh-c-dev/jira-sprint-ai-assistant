# Spring XD Dataset Provenance and Usage Audit

Status: **SOURCE OR LICENSING UNVERIFIED — DATA INTEGRATION BLOCKED**

Audit date: 2026-11-18

## 1. Source repositories

### Ayushi1429/Agile-Sprint-Analysis

- Repository: https://github.com/Ayushi1429/Agile-Sprint-Analysis
- Owner: Ayushi1429
- Repository status: public, not reported by GitHub as a fork
- Default branch: `main`
- Latest inspected commit: `17515a236ecf0d82510bcf875b6453094e3cf057`
- Latest commit date: 2025-06-27
- GitHub API `license`: `null`
- Repository `/license` endpoint: no licence file detected

The README describes the data as real-world Agile sprint data from the Spring XD open-source
project. It lists three public CSV files.

### RandulaKoralage/AgileScrumSprintVelocityDataSet

- Repository: https://github.com/RandulaKoralage/AgileScrumSprintVelocityDataSet
- Owner: RandulaKoralage
- Default branch: `master`
- Latest inspected commit: `544b07e3f36e9c42040609effae5b68a13fdefc9`
- Latest commit date: 2020-08-19
- GitHub API `license`: `null`
- Repository `/license` endpoint: no licence file detected
- Repository description: sprint velocity and story-point data for four open-source projects

The original repository README says the files were prepared in 2020 from Jira API/Greenhopper
data, with newlines, tabs and commas removed from strings, developer counts derived by assignee
counting, and "some inappropriate data" removed.

This repository is the better provenance source for collection methodology. The Ayushi repository
appears to be a later presentation/reanalysis repository rather than an established original
source. Byte identity was previously recorded for the three Spring XD files, but that statement
has not been reverified in this pass.

## 2. License and redistribution status

No licence file or machine-readable licence grant was found in either repository.

Public availability does not grant redistribution, modification or commercial use rights.
Accordingly:

- The real CSV files must not be downloaded into or committed into this repository.
- No download or redistribution permission is inferred.
- No use-right claim may be made.
- Synthetic fixtures remain the only permitted committed dataset rows.
- 

## 3. Candidate three source files

The Ayushi repository exposes these candidate files:

| Filename | Reported GitHub size | Git blob SHA | SHA-256 |
|---|---:|---|---|
| `Spring XD Issues 1992.csv` | 1,406,947 bytes | Available in GitHub API but not independently re-recorded | Not available; no file downloaded |
| `Spring XD Issues Summary 2861.csv` | 280,440 bytes | Available in GitHub API but not independently re-recorded | Not available; no file downloaded |
| `Spring XD Sprints 67.csv` | 6,583 bytes | Available in GitHub API but not independently re-recorded | Not available; no file downloaded |

No Git blob or SHA-256 value is asserted as a verified download checksum unless the raw file
is permitted and downloaded.

## 4. Expected transformation and repository policy

The original repository README records these source transformations:

- Newlines, tabs and commas removed from strings.
- Developer counts derived from assignee counts.
- Some inappropriate data removed.
- Data requires preprocessing.

These statements are source documentation, not independent verification.

This project must keep:

- `data/` real-data ignored unless explicitly authorized.
- synthetic fixtures under `src/test/resources/fixtures/`.
- no reconstructed or derived real records in tests.
- any later local-only dataset in `data/spring-xd/`.

## 5. Integration decision

Final verdict: **SOURCE OR LICENSING UNVERIFIED**

Stage 1 source discovery is complete, but Stage 2 download and all later stages are blocked.
No CSV was downloaded or written. No application code or domain mapping was changed.

Human approval is required for at least one of:

1. Written permission to use and redistribute the data.
2. A licence attached to both the source repository and the Spring XD-derived records.
3. A non-distributive approval that allows local analysis but prohibits committing raw rows.
