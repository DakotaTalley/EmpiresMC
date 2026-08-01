# Phase 9 — Commands & polish

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 8](phase-8-scepter-upgrades.md). Next: [Phase 10 — Exploit hardening & balance](phase-10-exploit-hardening-and-balance.md).

## Goal

The mod stops feeling like a dev build: discoverable commands, complete localization, sound/visual feedback everywhere, an in-game tutorial path, and the Scepter's real identity replacing the stick placeholder.

## Design decisions

- **Command tree** (player commands need no permission; admin subtree stays permission level 2):

  | Command | Purpose |
  |---|---|
  | `/empiresmc status` | Used/total chunks, tier, next-tier cost |
  | `/empiresmc claims` | List claimed chunks, **grouped by dimension**, with chunk and block coordinates |
  | `/empiresmc help` | Control cheat-sheet — resolve and print the player's *actual* bound keys, not hardcoded defaults, since keybinds are rebindable |
  | `/empiresmc admin claiminfo / profile / scepter / reload` | Phase 2/3/7 tools, consolidated |
  | `/empiresmc admin settier <player> <n>` / `grantclaims <player> <n>` / `forceunclaim` | Balance-testing and rescue tools |

- **`/empiresmc claims` is 1.0's answer to "where is my land."** After Phase 6's revision to local-only rendering, in-world borders show the chunk you occupy and the one you're targeting — nothing further out — so a late-game empire has claims the player has no in-world way to locate at all. That makes this command load-bearing rather than a convenience. The [market research](../research/market_research.md) calls a map overlay table stakes — but that verdict is calibrated to FTB Chunks, where claiming *happens on* the map; ours is positional, so the map is a locator, not a control surface. A grouped, coordinate-bearing list covers the locator job for a ≤39-chunk empire. Map-mod integration stays the first post-1.0 phase, not a 1.0 blocker.
- **Advancement tab as the tutorial:** a small chain (receive the Scepter → first claim → first denial seen → first upgrade → max tier) teaches the loop natively; the Phase 4 join message stays as the only push notification. Advancements are data files — no new systems. Flavor text must teach *controls*, not gestures, after the Phase 6 revision — and it cannot name specific keys, since they're rebindable and advancement text is static. Point at `/empiresmc help` and the HUD prompt instead; the HUD is now the primary teaching surface and the advancement chain the secondary one, which is a reversal of this bullet's original assumption.
- **Real Scepter art:** custom item model/texture replacing the stick, ideally with subtle per-tier visual variation (model predicate off synced tier). Art is allowed to be simple; distinct silhouette is the bar.
- **Audio identity:** distinct sounds for claim, unclaim, upgrade, and denial (vanilla sound events tuned by pitch — custom sounds are backlog). **Every one of these must be played through `util/PlayerSounds.playTo(player, sound, volume, pitch)`, never `player.playSound(...)`** — the latter, called server-side, plays to everyone *except* that player (`ServerLevel` turns the entity argument into `PlayerList.broadcast`'s exclusion argument), so in single-player it is silently inaudible. That bug shipped in Phase 4 and went unnoticed until the Phase 5 playtest; the seam exists specifically so this phase's pitch-tuning work can't reintroduce it. See the [Phase 5 doc](phase-5-protection-enforcement.md) for the full root cause.
- **ModMenu + Cloth Config screen ships in 1.0, as an optional dependency** (decided 2026-08-01; supersedes this phase's original "lean toward skipping"). The [market research](../research/market_research.md) lists a config screen among the things players judge a Fabric mod against, and names the closest mod-based competitor (Chunk By Chunk) as Mods-menu-configured. The JSON file at `config/empiresmc.json` remains the canonical, fully-supported path — the screen is a front-end over it, never a second source of truth.
  - **Optional, not required.** Compile against both, register the screen only when they're present, and declare them under `suggests` in `fabric.mod.json` — never `depends`. A user without ModMenu installed sees no change and loses nothing; the mod must load and run identically without either library.
  - **Client-only, per fabric `DEV-002`.** Both libraries are client-side, so the screen and its ModMenu entrypoint live in the client entrypoint package. A dedicated server must never classload any of it. This one is at least CI-visible: the gametest server runs headless, so a leak into common code fails the build loudly rather than silently — unlike this phase's other two deliverables.
  - **`SEC-005` disposition — accepted, with the real cost named.** The weight isn't the two artifacts; it's that neither is published to Maven Central or the Fabric maven, so this adds two third-party Maven repositories to `build.gradle.kts` (TerraformersMC for ModMenu, Shedaniel's for Cloth Config). That's a supply-chain surface the build doesn't currently have. Accepted because both are long-established, widely-depended-on Fabric ecosystem libraries and neither ships in the release jar. Pin exact versions rather than ranges, and verify coordinates and the optional-dependency wiring against the pinned Fabric/MC versions before committing — the specifics here are the shape of the solution, not a verified recipe.

## Steps

- [ ] Implement the command tree (Brigadier via Fabric API), all output translatable, admin subtree gated at permission level 2.
- [ ] Full localization audit: every user-facing string through `en_us.json`; in-dev sweep for raw translation keys (fabric `DEV-013` — missing keys render silently as raw keys, so grep + eyeball every screen/message).
- [ ] Advancement chain data files with fitting flavor text.
- [ ] Scepter model/texture (+ per-tier predicate if cheap); update `README` screenshots later in Phase 11.
  - **Asset licensing:** textures and sounds ship under **CC BY-SA 4.0**, not the code's MPL-2.0 ([decision](../research/licensing-decision.md)). Binary art can't carry an inline notice the way `.kt` files carry MPL Exhibit A, so drop a short `LICENSE` note in the texture/sound directories naming CC BY-SA 4.0 and the author. Otherwise anyone extracting a texture from the jar has no in-jar signal it's licensed differently from the code. Model/lang/recipe JSON are functional and stay MPL — the split is art only.
- [ ] Sound pass on all four core actions + denial throttle interplay (Phase 5) re-checked. Must be verified **by ear in a real client** — no gametest can observe it (see Risks below) — and every call site routed through `util/PlayerSounds`; grep for `playSound(` to confirm none crept back in.
- [ ] Add the two Maven repositories and both libraries as optional (`modCompileOnly` + dev-runtime) dependencies; confirm the release jar bundles neither and that `fabric.mod.json` lists them under `suggests`.
- [ ] Implement the ModMenu entrypoint + Cloth Config screen in the client package, generated from the Phase 7 schema so options can't drift between file and screen.
- [ ] Verify the mod loads and behaves identically with **neither** library present, and on a dedicated server — the no-optional-deps path is the supported default, not the fallback.
- [ ] Gametests: command outputs for status/claims match `ClaimService` truth; settier/grantclaims mutate correctly; forceunclaim bypasses cooldown but still refunds per config.

## Risks & flags

- **This phase's two headline deliverables both fail *silently* and invisibly to CI.** A sound that never reaches the client throws nothing and passes every gametest (Phase 4's sounds were dead for two phases before anyone heard the silence); a missing translation key renders as the raw key with no error (fabric `DEV-013`). Gametests run headless and server-side, so neither is automatable — budget real manual client time for the localization sweep and the sound pass rather than trusting a green build, and treat "CI green" as saying nothing about either.
- Command names/UX are API — renaming after 1.0 breaks muscle memory and any tutorials/videos; settle names in this phase deliberately.
- The advancement chain must not gate anything mechanical (pure tutorial) — gating claims behind advancements is a backlog idea for a "guided mode", not 1.0.
- Per-tier model predicates need the Phase 6 synced tier on the client — if the predicate plumbing fights back, ship one good model and move on; polish must not stall hardening.

## Exit criteria

- A player who has never read the README can install the mod, follow the advancement/messages, and reach their second tier unaided (hallway-test this on one person if possible).
- No raw translation keys anywhere; commands behave per table; CI green.
