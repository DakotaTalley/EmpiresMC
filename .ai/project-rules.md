# Project-Specific Rules

Rules this project has added that aren't part of the constitution's categories, layered on top of [`.ai/constitution.md`](constitution.md). Same rule format and tags — see the constitution for field/tag definitions.

- **ID format:** plain `PROJ-NNN` (sequential, no category segment) — unique within this file. Table membership is what makes these visibly distinct from template-sourced rules, not the ID.

## Rules

| ID | Title | Description | Mutability | Enforcement |
|---|---|---|---|---|
| `PROJ-001` | Verify Third-Party Source License Before Referencing | Before reading, adapting, or structurally mirroring code from a non-official third-party source (e.g. a GitHub repo turned up via search, as opposed to an official framework/library/docs repo), confirm that source's license and flag it to the user for explicit review before any code derived from it is committed — don't commit blind on the assumption a pattern is generic or public. | `mutable` | `stop` |
| `PROJ-002` | Phases and Stand-Alone Features Are Built on Dedicated Branches, Merged to Main via PR | Work on a plan phase (e.g. `plans/1.0-release/phase-*`) or a stand-alone feature starts on its own branch (not committed directly to `main`) and reaches `main` only through a pull request, even though the project currently has a single developer — this establishes the habit now so it doesn't need to be retrofitted once more developers contribute. | `mutable` | `warning` |

## Retired Rule IDs

_none yet_
