---
name: update-readme
description: Keep the project README in sync with the codebase. Use after a change affects how the project is installed, run, or used, or on direct request.
---

1. Diff the change against what the README currently documents: setup steps, scripts, dependencies, usage examples.
2. Update only the sections the change actually affects — don't do a general rewrite pass.
3. Apply `DOC-001`: document the non-obvious *why*, not what the code already shows.
4. If the change also needs deeper documentation (architecture, API reference) beyond what a README should hold, hand off to `update-docs` instead of overloading the README.
