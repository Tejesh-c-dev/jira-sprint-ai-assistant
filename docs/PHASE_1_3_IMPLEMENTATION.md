# Phases 1–3: Live Jira and assistant

The live integration is additive. The existing CSV loader, historical sprint endpoints, and metrics remain available. `APP_MODE=csv` is the default; set `APP_MODE=live` to enable the Jira refresh scheduler and manual Jira refresh.

## Local configuration

Copy `.env.example` to `.env` and supply Jira Cloud credentials, board ID, and optional OpenRouter credentials. Spring Boot does not automatically load `.env`; export those values into the process environment or configure them in the IDE before starting the application. Never commit `.env` or place API keys in source files. `.env` is ignored by Git.

The Jira account needs read access to the configured board and sprint issues. Jira access is read-only. `JIRA_STORY_POINTS_FIELD` defaults to `customfield_10016` and can be changed for sites using another field ID. OpenRouter uses `OPENROUTER_BASE_URL` (default `https://openrouter.ai/api/v1`) and `OPENROUTER_MODEL`.

## Live endpoints

- `GET /api/v1/live/sprint` — cached sprint snapshot and issue details
- `GET /api/v1/live/metrics` — sprint counts, story points, risk evidence, sync time and source status
- `GET /api/v1/live/team` — employee workload/support signals with reasons
- `POST /api/v1/live/refresh` — refresh immediately (only when `APP_MODE=live`)
- `GET /api/v1/live/status` — sync status and warning
- `POST /api/v1/assistant/ask` — request an evidence-based answer; body: `{ "question": "Why is this sprint at risk?" }`

The live snapshot is held in memory and refreshes on startup and every `JIRA_POLL_INTERVAL_MS` (default 60,000 ms). A failed Jira refresh retains the previous snapshot and marks its status unavailable. Story points are treated as zero for sums when the field is absent; the raw issue value remains null. Blocked work is detected from blocked status names and Jira issue links whose inward description indicates a blocking dependency. Overdue means the due date is before the server's current date and the issue is not complete.

Employee statuses use transparent rules in priority order: `BLOCKED`, `OVERLOADED`, `NEEDS_ATTENTION`, then `OK`. The API exposes `NO_ASSIGNED_WORK` as a supported status for future roster-backed signals; Jira sprint issue data alone does not provide a roster of people without assigned issues. Signals are workload/support indicators, not performance scores.

If OpenRouter is unconfigured or unavailable, the assistant returns a deterministic answer with `aiAvailable: false` and a warning. Its system prompt limits analysis to the supplied snapshot and requires human review for sensitive conclusions.

## Verification limits

Compile the project with Java 21 and Maven before connecting credentials. Jira and OpenRouter behavior still requires credentials and a reachable Jira board/provider for end-to-end verification. No real credentials belong in the repository.
