---
name: verify
description: Confirm a change actually works by exercising it end-to-end, not just via typecheck/tests. Use before committing a nontrivial change that has a runtime surface.
---

1. Identify the runtime surface the change affects — a CLI command, an API endpoint, a UI flow, a script.
2. Drive it directly (run the command, hit the endpoint, exercise the UI) with realistic inputs, not just the happy path.
3. Compare observed behavior against what was actually requested — "it didn't crash" is not verification.
4. If the change has no runtime surface (docs-only, a refactor already covered by passing tests), say so explicitly instead of fabricating a verification step.
5. Report pass/fail plainly. On failure, report what broke rather than attempting an unrelated fix.
