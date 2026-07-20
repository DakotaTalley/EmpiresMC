# Phase 1 — Project scaffolding

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Next: [Phase 2 — Claim data & persistence](phase-2-claim-data-and-persistence.md).

## Goal

A buildable, CI-verified Fabric + Kotlin mod skeleton that loads in the dev client, with licensing and test infrastructure settled before any gameplay code exists.

## Design decisions

- **Mod id:** `empiresmc` (conforms to Fabric id rules — see `.frameworks/fabric/rules.md` `DEV-001`).
- **Language:** Kotlin via `fabric-language-kotlin` (`"adapter": "kotlin"` on entrypoints). This also bundles `kotlinx.serialization`, which Phase 7 uses for config — no extra dependency later.
- **License: LGPL-3.0-or-later.** The project wants copyleft. LGPL over GPL because a Minecraft mod links against Mojang's proprietary code and is redistributed inside modpacks — LGPL keeps the mod's own source copyleft without raising GPL linking/distribution questions for those cases. MPL-2.0 is the fallback if weaker file-level copyleft is ever preferred. Record the final choice in `fabric.mod.json`'s `license` field and the README.
- **Mappings:** Yarn (Fabric ecosystem default). Mojmap is an acceptable alternative; decide once here and don't churn.
- **Source sets:** single source set with client-only code isolated behind the client entrypoint package. Loom's `splitEnvironmentSourceSets()` adds wiring complexity (fabric `DEV-008`) for little benefit at this size — revisit via backlog if client leakage bugs appear.
- **Java/JDK:** whatever the pinned Minecraft version requires (Java 21 for the 1.21.x line).

## Steps

- [ ] Pin versions and record them in this section when chosen: Minecraft (latest stable at phase start), Fabric Loader, Fabric API, fabric-language-kotlin, Loom, Kotlin, JDK.
  - Chosen versions: _record here_
- [ ] Initialize Gradle project (Kotlin DSL, `build.gradle.kts`) with Fabric Loom; commit the Gradle wrapper and `.gitignore` (Gradle, IDE, `run/`).
- [ ] Write `fabric.mod.json`: `schemaVersion: 1` first, `id: empiresmc`, version from Gradle, `environment: "*"`, Kotlin-adapter entrypoints, `depends` on `fabricloader`, `fabric-api`, `fabric-language-kotlin`, `minecraft` version range.
- [ ] Create entrypoints as Kotlin objects: `EmpiresMC` (`ModInitializer`) and `EmpiresMCClient` (`ClientModInitializer`), each logging an init line via a shared mod logger.
- [ ] Add the registry-holder pattern: an object with an explicit `initialize()` called from the main entrypoint, even while empty (fabric `DEV-005` — fields only register when the class is statically initialized).
- [ ] Add `LICENSE` (LGPL-3.0-or-later full text), `license` field in `fabric.mod.json`, license section in README.
- [ ] Wire unit testing: JUnit Platform (`useJUnitPlatform()`) + Fabric Loader JUnit dependency, plus a shared test helper that bootstraps registries (`SharedConstants` detect + `Bootstrap`) per fabric `DEV-009`; one passing smoke test.
- [ ] Wire the Fabric gametest API with a `runGametest`-style headless run task; one trivial passing gametest.
- [ ] GitHub Actions workflow: build + unit tests + gametests on push and PR.
- [ ] README skeleton: one-paragraph pitch, dev environment setup, license note.

## Risks & flags

- **License is a public commitment** — changing it after outside contributions arrive requires contributor consent. Settle it in this phase, before the repo attracts attention.
- Version pinning drifts: Fabric API and fabric-language-kotlin versions are coupled to the Minecraft version. Record the exact set above so later phases don't guess.
- Mid-development Minecraft version bumps are a real cost (mappings + API churn). Policy: stay on the pinned version through 1.0 unless a blocking bug forces a bump; port after release.

## Exit criteria

- `./gradlew build test` and the gametest task pass locally and in CI.
- Dev client launches and the log shows both entrypoint init lines.
- `LICENSE` committed; `fabric.mod.json` validates (mod appears in the mod list in-game).
