# Phase 1 — Project scaffolding

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Next: [Phase 2 — Claim data & persistence](phase-2-claim-data-and-persistence.md).

## Goal

A buildable, CI-verified Fabric + Kotlin mod skeleton that loads in the dev client, with licensing and test infrastructure settled before any gameplay code exists.

## Design decisions

- **Mod id:** `empiresmc` (conforms to Fabric id rules — see `.frameworks/fabric/rules.md` `DEV-001`).
- **Language:** Kotlin via `fabric-language-kotlin` (`"adapter": "kotlin"` on entrypoints). This also bundles `kotlinx.serialization`, which Phase 7 uses for config — no extra dependency later.
- **License: LGPL-3.0-or-later.** The project wants copyleft. LGPL over GPL because a Minecraft mod links against Mojang's proprietary code and is redistributed inside modpacks — LGPL keeps the mod's own source copyleft without raising GPL linking/distribution questions for those cases. MPL-2.0 is the fallback if weaker file-level copyleft is ever preferred. Record the final choice in `fabric.mod.json`'s `license` field and the README.
- **No CLA or DCO — implicit inbound=outbound licensing.** Contributions are accepted under the project's current license (LGPL-3.0-or-later) with no separate rights grant to the maintainer. A CLA would ease future relicensing but also hands the maintainer unilateral power to relicense toward something more restrictive later (the mechanism behind MongoDB/Elastic/HashiCorp/Redis moving off open-source licenses) — that trade-off runs against the project's copyleft goal, so it's skipped. Cost: relicensing after external contributions land requires tracking down every contributor for consent.
- **Mappings:** N/A — moot as of Minecraft `26.1`. Mojang dropped obfuscation entirely starting with the `26.1` cycle (the year.month release scheme); `1.21.11` was the final Yarn-supporting release, and there is no Mojmap dependency to declare either. `build.gradle.kts` has no `mappings(...)` line at all.
- **Source sets:** single source set with client-only code isolated behind the client entrypoint package. Loom's `splitEnvironmentSourceSets()` adds wiring complexity (fabric `DEV-008`) for little benefit at this size — revisit via backlog if client leakage bugs appear.
- **Java/JDK:** whatever the pinned Minecraft version requires. Minecraft dropped the `1.2x.x` scheme for year.month releases (`26.1`, `26.2`, ...) in early 2026; the `26.2` line requires JDK 25 (LTS, GA September 2025), not the JDK 21 this line originally assumed.

## Steps

- [x] Pin versions and record them in this section when chosen: Minecraft (latest stable at phase start), Fabric Loader, Fabric API, fabric-language-kotlin, Loom, Kotlin, JDK.
  - Chosen versions (as of 2026-07-20):
    - Minecraft: `26.2` ("Chaos Cubed", released 2026-06-16 — latest stable)
    - Fabric Loader: `0.19.3` (released 2026-06-01)
    - Fabric API: `0.155.2+26.2` (released 2026-07-17, targets MC `26.2`)
    - fabric-language-kotlin: `1.13.13+kotlin.2.4.10` (released 2026-07-15, bundles Kotlin `2.4.10`)
    - Fabric Loom: `1.17` (released 2026-06-07; updated internally to Gradle 9.5, so the Gradle wrapper pinned in the next step should be 9.5+)
    - Kotlin: `2.4.10` (via fabric-language-kotlin's bundled version — don't pin a separate Kotlin Gradle plugin version that could drift from this)
    - JDK: `25` (LTS; minimum Java version required by Minecraft `26.2`)
- [x] Initialize Gradle project (Kotlin DSL, `build.gradle.kts`) with Fabric Loom; commit the Gradle wrapper and `.gitignore` (Gradle, IDE, `run/`).
  - Uses plugin id `net.fabricmc.fabric-loom` (the `fabric-loom` short alias breaks mappings resolution on unobfuscated MC — see note below), `kotlin("jvm")` at `2.4.10`, and JDK 25 via `kotlin { jvmToolchain(25) }`.
  - Gradle wrapper pinned to `9.6.1` (Loom 1.17.x requires 9.5+).
  - `./gradlew build` passes (no sources yet — `fabric.mod.json` and entrypoints land in the next steps).
  - **Mappings design decision superseded:** Minecraft dropped Yarn entirely and ships unobfuscated as of `26.1` (final Yarn-supporting release was `1.21.11`). No `mappings(...)` dependency is used or needed. Fabric API and fabric-language-kotlin are also plain `implementation`, not `modImplementation` — the `mod*` remapping configurations aren't needed against an unobfuscated game.
- [x] Write `fabric.mod.json`: `schemaVersion: 1` first, `id: empiresmc`, version from Gradle, `environment: "*"`, Kotlin-adapter entrypoints, `depends` on `fabricloader`, `fabric-api`, `fabric-language-kotlin`, `minecraft` version range.
  - Entrypoints reference `com.dakotatalley.empiresmc.EmpiresMC` (main) and `com.dakotatalley.empiresmc.client.EmpiresMCClient` (client) — classes don't exist yet, created in the next step.
  - `depends`: `fabricloader >=0.19.3`, `minecraft ~26.2`, `java >=25`, `fabric-api *`, `fabric-language-kotlin >=1.13.13+kotlin.2.4.10`.
  - `license` field intentionally omitted here — added together with the `LICENSE` file in the dedicated licensing step below.
  - Verified `${version}` expands correctly to `0.1.0-SNAPSHOT` in the built jar via `./gradlew build`.
- [x] Create entrypoints as Kotlin objects: `EmpiresMC` (`ModInitializer`) and `EmpiresMCClient` (`ClientModInitializer`), each logging an init line via a shared mod logger.
  - `src/main/kotlin/com/dakotatalley/empiresmc/EmpiresMC.kt` — holds `MOD_ID` and the shared SLF4J `LOGGER` (via `org.slf4j.LoggerFactory`, no extra dependency needed — it's already on the game's runtime classpath).
  - `src/main/kotlin/com/dakotatalley/empiresmc/client/EmpiresMCClient.kt` — client-only package per the single-source-set design decision; logs through `EmpiresMC.LOGGER`.
  - Verified via `./gradlew build`: `compileKotlin` succeeds and the built jar contains both class files at the exact paths `fabric.mod.json`'s entrypoints reference.
- [x] Add the registry-holder pattern: an object with an explicit `initialize()` called from the main entrypoint, even while empty (fabric `DEV-005` — fields only register when the class is statically initialized).
  - `src/main/kotlin/com/dakotatalley/empiresmc/registry/ModRegistry.kt` — empty `object` with an `initialize()` method, ready for later phases' `static final` registry fields.
  - Called first thing in `EmpiresMC.onInitialize()`, before the log line. Verified via `./gradlew build`.
- [x] Add `LICENSE` (LGPL-3.0-or-later full text), `license` field in `fabric.mod.json`, license section in README.
  - `LICENSE` — canonical GNU LGPLv3 body text, pulled via `gh api /licenses/lgpl-3.0` (GitHub/SPDX-recognized text; same body serves both `LGPL-3.0-only` and `LGPL-3.0-or-later` — the "or later" designation lives in the SPDX identifier used elsewhere, not the body wording).
  - `fabric.mod.json`: added `"license": "LGPL-3.0-or-later"`.
  - `README.md`: added a License section linking to `LICENSE`.
  - Verified via `./gradlew build`: build succeeds, `fabric.mod.json`'s `license` field reads back as `LGPL-3.0-or-later` from the built jar, and `LICENSE` is packaged into the jar as `LICENSE_empiresmc` (per the existing `tasks.jar` rename rule).
- [x] Wire unit testing: JUnit Platform (`useJUnitPlatform()`) + Fabric Loader JUnit dependency, plus a shared test helper that bootstraps registries (`SharedConstants` detect + `Bootstrap`) per fabric `DEV-009`; one passing smoke test.
  - `build.gradle.kts`: added `testImplementation("net.fabricmc:fabric-loader-junit:${loader_version}")` (transitively brings JUnit Jupiter — confirmed via its POM, matches the pattern used by Fabric API's own `build.gradle`) and `tasks.test { useJUnitPlatform() }`. No separate JUnit BOM/version needed.
  - `src/test/kotlin/com/dakotatalley/empiresmc/test/MinecraftBootstrapExtension.kt` — reusable JUnit 5 `BeforeAllCallback` extension calling `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap()` exactly once per JVM, applied via `@ExtendWith(MinecraftBootstrapExtension::class)`. Idempotency uses JUnit 5's own documented one-time-global-setup idiom (a `CloseableResource` stored in the root `ExtensionContext.Store`), not a hand-rolled lock. Vanilla class paths (`net.minecraft.SharedConstants`, `net.minecraft.server.Bootstrap`) sourced from fabric rule `DEV-009` and verified present in the local MC `26.2` jar (`.gradle/caches/fabric-loom/26.2/minecraft-merged.jar`) — no third-party repo code referenced.
  - `src/test/kotlin/com/dakotatalley/empiresmc/EmpiresMCTest.kt` — smoke test: one checks `MOD_ID` matches Fabric's id regex, the other reads `net.minecraft.world.item.Items.STONE` (a real vanilla registry entry) to prove the bootstrap extension actually initialized registries, not just that a no-op didn't throw.
  - Verified via `./gradlew build test --rerun`: both tests pass (`test-results` XML shows `tests="2" failures="0" errors="0"`).
- [x] Wire the Fabric gametest API with a `runGametest`-style headless run task; one trivial passing gametest.
  - Researched against official sources only (per `PROJ-001`): `FabricMC/fabric-api`'s own `26.2`-branch `build.gradle`/gametest module source (Apache-2.0), and the official `FabricMC/fabric-docs` `automatic-testing.md` + `loom/fabric-api.md` pages.
  - `build.gradle.kts`: added `fabricApi { configureTests { createSourceSet = true; modId = "empiresmc-test"; enableGameTests = true; enableClientGameTests = false; eula = true } }` — no extra plugin needed, this DSL comes from the already-applied `net.fabricmc.fabric-loom` plugin. `eula = true` is required boilerplate to let Loom run a real dedicated test server (agrees to the standard Minecraft EULA every Fabric dev already operates under).
  - `src/gametest/kotlin/com/dakotatalley/empiresmc/gametest/EmpiresMCGameTest.kt` — one `@GameTest` method asserting `EmpiresMC.MOD_ID` is loaded via `FabricLoader`, using the default empty structure (no custom NBT needed).
  - `src/gametest/resources/fabric.mod.json` — separate `empiresmc-test` mod id, `fabric-gametest` entrypoint pointing at the class above. Kept out of the main mod jar since it's its own Loom-generated source set.
  - Verified via `./gradlew build test runGameTest`: task `runGameTest` (the headless run task) launches a real dedicated test server, logs `empiresmc-test:empires_mcgame_test_mod_is_loaded`, and passes. Sanity-checked the wiring is real (not a false positive) by temporarily breaking the assertion — confirmed `runGameTest` fails the build with `1 required tests failed`, then reverted.
  - **"2 GAME TESTS COMPLETE" with only 1 test declared — expected, not a bug.** Isolated empirically by temporarily emptying our `fabric-gametest` entrypoint array: the baseline is 1 test even with zero of ours registered. That baseline test is `net.minecraft.gametest.framework.GeneratedTest` — vanilla Minecraft's own GameTest framework auto-generates one lightweight sanity test per registered `TestEnvironmentDefinition` (here, the default `minecraft:default` environment every `@GameTest` uses unless `environment()` is overridden), to confirm the environment setup itself applies correctly. It's independent of any mod code, always passes, and isn't something we need to silence or account for — just don't be surprised when the total test count is "declared tests + 1".
- [x] GitHub Actions workflow: build + unit tests + gametests on push and PR.
  - `.github/workflows/build.yml` — sourced from `FabricMC/fabric-example-mod`'s official, current `26.2`-branch workflow (per `PROJ-001`), plus the "store reports on failure" step from the official Fabric docs' automatic-testing guide.
  - Single `./gradlew build` step covers all three: confirmed via `./gradlew build --rerun-tasks` that `build` already depends on `test` (unit tests) and `runGameTest` (gametests) through the `check` task — no separate CI steps needed for each.
  - Action versions (`actions/checkout@v6`, `gradle/actions/wrapper-validation@v6`, `actions/setup-java@v5`, `actions/upload-artifact@v7`) pulled live from the upstream template rather than assumed from memory, then each tag verified to actually resolve via the GitHub API before committing to them.
  - Triggers on `[pull_request, push]`, runs on `ubuntu-24.04`, JDK 25 (Microsoft Build of OpenJDK) matching the toolchain pinned in `build.gradle.kts`.
- [x] README skeleton: one-paragraph pitch, dev environment setup, license note.
  - `README.md` — one-paragraph pitch, a Development Setup section (JDK 25 requirement, clone + `./gradlew build`, plus `runClient`/`test`/`runGameTest` pointers), and the existing License section.
  - `runClient` (and `runServer`) confirmed present via `./gradlew tasks --all` before being referenced — these come free from the `fabric-loom` plugin, no explicit config needed.
  - Verified via `./gradlew build --rerun-tasks`: full clean build succeeds (compiles, unit tests, gametests, jar).

## Risks & flags

- **License is a public commitment** — changing it after outside contributions arrive requires contributor consent. Settle it in this phase, before the repo attracts attention.
- Version pinning drifts: Fabric API and fabric-language-kotlin versions are coupled to the Minecraft version. Record the exact set above so later phases don't guess.
- Mid-development Minecraft version bumps are a real cost (mappings + API churn). Policy: stay on the pinned version through 1.0 unless a blocking bug forces a bump; port after release.

## Exit criteria

- [x] `./gradlew build test` and the gametest task pass locally. (CI verification pending first push — workflow is written and its action versions/tags verified, but hasn't executed on GitHub yet.)
- [x] Dev client launches and the log shows both entrypoint init lines. Verified via `./gradlew runClient`: `EmpiresMC common initializer running` and `EmpiresMC client initializer running` both logged, client reached the title screen cleanly (no crash report generated) before being stopped. An unrelated `UnsatisfiedLinkError` for `libflite.so` appeared — Minecraft's own Linux narrator/TTS library, missing on this machine and caught internally by vanilla's own narrator init; not a mod issue.
  - Incidentally found `.fabric/` and `logs/` were being generated at the project root, untracked and not covered by `.gitignore`'s existing `run/` entry — added both.
- [x] `LICENSE` committed; `fabric.mod.json` validates (mod appears in the mod list in-game).
  - `fabric.mod.json` validates — confirmed via the `runClient` log listing `empiresmc 0.1.0-SNAPSHOT` among loaded mods, and structurally checked against `DEV-001` (schemaVersion first, conformant id, fully-qualified Kotlin-adapter entrypoints).
  - `LICENSE` committed, and verified byte-for-byte identical to the canonical LGPL-3.0 text (sha256 match against `gh api /licenses/lgpl-3.0`).
  - Phase 1 committed as one commit per step. The Gradle wrapper jar was checksum-verified against Gradle's published `gradle-9.6.1-wrapper.jar.sha256` before commit, so CI's `wrapper-validation` step has a known-good input.
