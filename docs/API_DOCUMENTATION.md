# API Documentation

## Sprint endpoints

- `GET /api/v1/sprints`
- `GET /api/v1/sprints/{sprintId}`
- `GET /api/v1/sprints/{sprintId}/analysis`
- `GET /api/v1/sprints/{sprintId}/health`

## Comparison and trend endpoints

- `GET /api/v1/sprints/compare?sprintIds=1&sprintIds=2`
- `GET /api/v1/sprints/trends`

Unknown IDs in comparisons are omitted. Missing `sprintIds` returns an empty comparison with
limitations.

## Dataset endpoints

- `GET /api/v1/dataset/status`
- `GET /api/v1/dataset/quality`

## Response rules

- Numeric zero is a valid value.
- Missing values serialize as JSON `null`.
- Unsupported metrics retain an availability state and null value.
- Analysis responses preserve metric reasons, source fields and finding codes.
- Dataset status separates load outcome from usability.
