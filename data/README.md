# Data directory — local dataset root

This directory is the default local dataset root for the Spring XD Sprint Health MVP.
**Only this README is tracked in Git.** Every other file placed here is ignored by
`.gitignore` (`/data/*`, with an exception for this README).

## Expected local files

Place the three Spring XD CSV inputs here before running the data layer:

| File | Contents |
|---|---|
| `issues.csv` | Issue master records |
| `issue_sprint_summary.csv` | Issue-to-sprint summary rows |
| `sprints.csv` | Sprint metadata |

These names are the current expectation; the loader configuration (DATA-08) makes both the
directory and the file names configurable.

## Prohibition on committing original CSVs

Do **not** commit, push or otherwise redistribute the original Spring XD CSV files.

- The upstream repository does not state a clear licence for redistribution.
- Reuse/redistribution permission has not been confirmed by a human.
- Public availability does not imply a right to redistribute.

Until permission is explicitly confirmed, keep the real files only in your local working
copy; they stay untracked because of the ignore rules above. Small, clearly labelled
**synthetic** fixtures live under `src/test/resources/fixtures/` and are the only CSV data
that may be committed.

See `docs/DATA_MAPPING.md` §1 for dataset attribution and reuse-term status.
