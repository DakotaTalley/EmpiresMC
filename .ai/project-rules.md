# Project-Specific Rules

Rules this project has added that aren't part of the constitution's categories, layered on top of [`.ai/constitution.md`](constitution.md). Same rule format and tags — see the constitution for field/tag definitions.

- **ID format:** plain `PROJ-NNN` (sequential, no category segment) — unique within this file. Table membership is what makes these visibly distinct from template-sourced rules, not the ID.

## Rules

| ID | Title | Description | Mutability | Enforcement |
|---|---|---|---|---|
| `PROJ-003` | Sync Guidance at Phase/Feature Start | Before starting feature-specific work on a new plan phase or stand-alone feature branch, run `sync-guidance` and commit its result as an isolated first commit on that branch (e.g. `chore: sync guidance`) before any feature commits, so guidance stays current without template diffs mixing into feature work. | `mutable` | `warning` |

## Retired Rule IDs

`PROJ-001`, `PROJ-002` — superseded by `SEC-006` and `DEV-005`, which the template now ships with equivalent text.
