---
name: plan-feature
description: Generate an implementation plan for a single new feature and save it under a project-root plans/ folder. Use when starting a non-trivial feature that warrants a written plan before implementation.
---

1. Create the `plans/` folder at the project root if it doesn't already exist.
2. Slug the feature name (kebab-case) and write the plan to `plans/<feature-slug>.md`.
3. Cover: goal/scope, approach, critical files, open questions — persisted as a file instead of only conversation output.
4. Write the approach's concrete steps as a markdown checklist (`- [ ] step`), not prose or a numbered list — this is what lets progress be tracked as implementation proceeds. As each step is completed during implementation, check it off (`- [x]`) in this file rather than leaving the plan static.
5. If the plan turns out to need multiple phases with substantial detail each, stop and switch to the `phased-plan` skill instead of continuing to grow this single file.
