# Phase 11 — Release

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 10 — Exploit hardening & balance](phase-10-exploit-hardening-and-balance.md).

## Goal

EmpiresMC 1.0.0 is published, documented, and installable by a stranger from Modrinth in under five minutes.

## Design decisions

- **Versioning: semver.** 1.0.0 at release; patch releases for fixes on the pinned Minecraft version; Minecraft-version ports bump minor.
- **Distribution: Modrinth primary** (best Fabric-community reach, clean license metadata), CurseForge optional mirror in the same phase if the upload friction is low. GitHub Releases carries the canonical jars either way.
- **License follow-through:** MIT governs the repo (chosen in Phase 1, relicensed from LGPL-3.0-or-later during Phase 6 on the [market research](../research/market_research.md)'s modpack-adoption finding); release adds the visible surface — license in mod metadata, README badge, source link on the mod page. The sources-jar and source-discoverability obligations that copyleft *required* are now optional, but keep them: they cost nothing and modpack authors read them as a trust signal.
- **Positioning copy is a deliverable, not an afterthought.** The research's sharpest go-to-market findings are all page-copy: (1) the reflex "why do I need claims in single-player?" will bounce readers who file this under griefing protection, so the page must lead with the progression hook and never call itself a claim mod; (2) tag as Game Mechanics / Adventure, not Utility / Management; (3) name collision with a live Towny server and with fWhip's Empires SMP needs an explicit disambiguating line near the top (see Phase 6's name review — this may be moot if the rename lands); (4) state single-player support explicitly, since the whole comparable set does. Write this before the gallery, not after.

## Steps

- [ ] Final README: feature overview with screenshots/GIF (claim borders in view), install instructions (Fabric Loader + Fabric API + fabric-language-kotlin prerequisites, plus ModMenu + Cloth Config called out clearly as **optional** — installed, you get an in-game settings screen; absent, you edit the JSON file and nothing else differs), full config reference generated from the Phase 7 schema, known-issues section (the "allow (documented)" rows from the Phase 10 register), license section.
  - Known-issues must name the **modded-item enforcement gap** explicitly (Phase 5's deny-list is class-based; `wildDenyItems` is the escape hatch). The research found that modpack authors specifically look for this disclosure, and that undocumented protection bypasses are the #1 recurring technical complaint across the claim-mod category.
  - Include a short **compatibility statement** on what we deliberately *don't* touch: claims never force-load or unload chunks, so portal-linked farms and chunk-loading behave exactly as vanilla; mob griefing and explosions are not coupled to claims. Both are direct answers to documented FTB Chunks irritants (issue #329, the fluid/fire/piston perf-gated toggles) and read as differentiators rather than omissions.
- [ ] `CONTRIBUTING.md`: dev setup, test expectations (unit + gametest green before PR), the plan-docs convention (this folder), MIT inbound=outbound contribution note (no CLA/DCO, per Phase 1).
- [ ] `CHANGELOG.md` seeded with 1.0.0 highlights.
- [ ] Release CI: tag push → build → attach jar + sources jar to a GitHub Release.
- [ ] Modrinth project: description, gallery, license field, source/issues links, dependency declarations; upload 1.0.0. CurseForge mirror if pursued.
- [ ] Clean-instance QA: fresh Prism/vanilla-launcher instance, only the three prerequisites + EmpiresMC — full smoke of the loop (claim, deny, upgrade, visualize, config edit, reload).
- [ ] Compatibility smoke with the usual suspects players will add anyway: Sodium, Lithium, Iris (rendering and tick-hook interactions are the risk points; Phase 6 render code is the likely friction).
- [ ] Optional-dependency smoke, both directions: with ModMenu + Cloth Config installed (screen opens, edits persist, `/empiresmc admin reload` and the screen agree) **and** with neither installed (mod loads, config file path works, nothing logs an error). The second case is the default install and the one most likely to go untested, since dev runtime will have both.
- [ ] Tag `v1.0.0`, publish, announce wherever appropriate.
- [ ] Post-release: triage the [backlog](backlog.md) into candidate post-1.0 phases (new phase files + summary rows per the plan convention).

## Risks & flags

- Sodium/Iris rendering incompatibilities discovered this late would be painful — if Phase 6 used only Fabric API world-render events (as planned) the risk is low, but verify early in the phase, not on release day.
- Mod-page copy sets expectations: state clearly that 1.0 targets single-player; multiplayer behavior is unsupported-but-unblocked (the Phase 2 UUID keying means it may largely work — promise nothing).
- Support surface opens at publish: issues will arrive against configs and modpacks never tested — the Phase 7 "never crash on bad config / missing ids" guarantees are the shield; make sure they held.

## Exit criteria

- 1.0.0 live on Modrinth and GitHub Releases with sources; clean-instance QA passed; README/config reference accurate against shipped defaults.
