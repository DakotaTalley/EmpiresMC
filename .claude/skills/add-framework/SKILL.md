---
name: add-framework
description: Scaffold a new .frameworks/<name>/ rule set for a framework not yet covered. Use when adding support for a framework beyond the initial dotnet/react/next/rust/godot set.
---

1. Create `.frameworks/<name>/rules.md` using `.ai/constitution.md`'s rule table format (ID, Title, Description, Mutability, Enforcement).
2. Use the same `<CATEGORY>-NNN` ID format as every other rule set (e.g. `SEC-001`) — no framework prefix needed, since this file is already the namespace. Document that at the top of the new file, same as the other `.frameworks/*/rules.md` files.
3. Document a one-line detection signal — a manifest file, config file, or dependency that identifies this framework in a project — so the setup script and agents can auto-detect it, following the pattern in [`README.md`](../../../README.md) / [`plans/ai-guidance-template.md`](../../../plans/ai-guidance-template.md) Phase 4.
4. Register the new framework wherever frameworks are enumerated: `README.md`, and the detection logic in `.ai/setup.sh`.
5. Seed the file with a header linking back to `.ai/constitution.md`, an empty rules table (a single placeholder row: `| _none yet_ | | | | |`), and an empty **Retired Rule IDs** section (`_none yet_`) — same shape as the other `.frameworks/*/rules.md` files — for `add-rule` to populate later. `add-rule` replaces this placeholder row (it checks for this exact text) rather than adding a second row below it. Don't invent rule content unprompted.
