---
name: phased-plan
description: Generate a phased plan for a larger, multi-phase effort — a short top-level summary plus a dedicated document per phase. Use when an effort spans multiple phases with enough detail per phase that one document would grow unwieldy.
---

1. Create `plans/` at the project root if it doesn't already exist (shared with `plan-feature`).
2. Slug the initiative name and write `plans/<initiative-slug>.md`: a short summary listing each phase (name + one-line scope) — nothing more.
3. Create an adjacent folder `plans/<initiative-slug>/` and write one file per phase: `plans/<initiative-slug>/phase-N-<phase-name>.md`, holding that phase's full detail.
4. Within each phase file, write that phase's concrete steps as a markdown checklist (`- [ ] step`), not prose or a numbered list — this is what lets progress be tracked as implementation proceeds. As each step is completed during implementation, check it off (`- [x]`) in the phase file rather than leaving the plan static.
5. Link each phase from the summary doc, and link back to the summary from each phase doc.
6. As phases get detailed further, edit only the relevant phase file — keep the summary short. This template's own [`plans/ai-guidance-template.md`](../../../plans/ai-guidance-template.md) is the pre-split example this skill is meant to formalize.
