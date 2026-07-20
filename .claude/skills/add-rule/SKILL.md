---
name: add-rule
description: Author a new rule for .ai/constitution.md, .ai/project-rules.md, or a .frameworks/<name>/ rule set. Use when adding new development-practice, security, project-specific, or framework-specific guidance as a formal, tagged rule rather than ad hoc prose.
---

1. Determine the target file: `.ai/constitution.md` for general template-sourced guidance, `.ai/project-rules.md` for a project-specific rule, or the relevant `.frameworks/<name>/rules.md` for framework-specific guidance.
   - `.ai/project-rules.md` is project-owned and not shipped by the template — if it doesn't exist yet in this project, create it first with this skeleton:
     ```markdown
     # Project-Specific Rules

     Rules this project has added that aren't part of the constitution's categories, layered on top of [`.ai/constitution.md`](constitution.md). Same rule format and tags — see the constitution for field/tag definitions.

     - **ID format:** plain `PROJ-NNN` (sequential, no category segment) — unique within this file. Table membership is what makes these visibly distinct from template-sourced rules, not the ID.

     ## Rules

     | ID | Title | Description | Mutability | Enforcement |
     |---|---|---|---|---|
     | _none yet_ | | | | |

     ## Retired Rule IDs

     _none yet_
     ```
2. Determine the ID and table:
   - A rule belonging to one of this template's existing categories: ID is `<CATEGORY>-NNN` (e.g. `SEC-006`, no extra prefix — the table it's in is the only namespace it needs) in `.ai/constitution.md`, in that category's table.
   - A rule the *project itself* is adding: ID is a plain `PROJ-NNN` (sequential, no category segment) in `.ai/project-rules.md`'s **Rules** table, regardless of what it's about — never folded into `SEC`/`DEV`/`DOC` just because it's thematically related.
   - A framework rule: same `<CATEGORY>-NNN` format, in that framework's `.frameworks/<name>/rules.md` file — no framework prefix either, since the file it's in is already scoped to that framework. IDs are only unique within a file, not globally (`SEC-001` in `dotnet/rules.md` and `SEC-001` in `react/rules.md` are different rules).
3. Find the next unused number for that ID within the target file (e.g. `SEC-006`, `PROJ-003`) by scanning both its table *and* the file's **Retired Rule IDs** list. Never reuse a retired ID — the ledger is what makes that enforceable even after a row has been deleted from the visible table.
4. Gather: Title (short, human-readable), Description (one or two sentences — what the rule requires and why), Mutability tag (`immutable` or `mutable` — ask if not specified, don't default silently), Enforcement tag (`info`, `warning`, `stop`, or a project-defined extra tag if the target file already has one).
5. Append a new row to the correct table, preserving column order and Markdown table formatting. If a table currently only has the `_none yet_` placeholder row, replace that row rather than adding below it.
6. Report the new rule's full ID and which table it landed in back to the caller.
