# Phase 11 — Release

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 10 — Exploit hardening & balance](phase-10-exploit-hardening-and-balance.md).

## Goal

EmpiresMC 1.0.0 is published, documented, and installable by a stranger from Modrinth in under five minutes.

## Design decisions

- **Versioning: semver.** 1.0.0 at release; patch releases for fixes on the pinned Minecraft version; Minecraft-version ports bump minor.
- **Distribution: Modrinth primary** (best Fabric-community reach, clean license metadata), CurseForge optional mirror in the same phase if the upload friction is low. GitHub Releases carries the canonical jars either way.
- **License follow-through:** LGPL-3.0-or-later already governs the repo (Phase 1); release adds the visible surface — license in mod metadata, README badge, source link on the mod page (copyleft obliges keeping the source discoverable).

## Steps

- [ ] Final README: feature overview with screenshots/GIF (claim borders in view), install instructions (Fabric Loader + Fabric API + fabric-language-kotlin prerequisites), full config reference generated from the Phase 7 schema, known-issues section (the "allow (documented)" rows from the Phase 10 register), license section.
- [ ] `CONTRIBUTING.md`: dev setup, test expectations (unit + gametest green before PR), the plan-docs convention (this folder), LGPL contribution note.
- [ ] `CHANGELOG.md` seeded with 1.0.0 highlights.
- [ ] Release CI: tag push → build → attach jar + sources jar to a GitHub Release (sources jar matters under LGPL).
- [ ] Modrinth project: description, gallery, license field, source/issues links, dependency declarations; upload 1.0.0. CurseForge mirror if pursued.
- [ ] Clean-instance QA: fresh Prism/vanilla-launcher instance, only the three prerequisites + EmpiresMC — full smoke of the loop (claim, deny, upgrade, visualize, config edit, reload).
- [ ] Compatibility smoke with the usual suspects players will add anyway: Sodium, Lithium, Iris (rendering and tick-hook interactions are the risk points; Phase 6 render code is the likely friction).
- [ ] Tag `v1.0.0`, publish, announce wherever appropriate.
- [ ] Post-release: triage the [backlog](backlog.md) into candidate post-1.0 phases (new phase files + summary rows per the plan convention).

## Risks & flags

- Sodium/Iris rendering incompatibilities discovered this late would be painful — if Phase 6 used only Fabric API world-render events (as planned) the risk is low, but verify early in the phase, not on release day.
- Mod-page copy sets expectations: state clearly that 1.0 targets single-player; multiplayer behavior is unsupported-but-unblocked (the Phase 2 UUID keying means it may largely work — promise nothing).
- Support surface opens at publish: issues will arrive against configs and modpacks never tested — the Phase 7 "never crash on bad config / missing ids" guarantees are the shield; make sure they held.

## Exit criteria

- 1.0.0 live on Modrinth and GitHub Releases with sources; clean-instance QA passed; README/config reference accurate against shipped defaults.
