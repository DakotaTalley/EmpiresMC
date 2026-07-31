# Phase 9 — Commands & polish

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 8](phase-8-scepter-upgrades.md). Next: [Phase 10 — Exploit hardening & balance](phase-10-exploit-hardening-and-balance.md).

## Goal

The mod stops feeling like a dev build: discoverable commands, complete localization, sound/visual feedback everywhere, an in-game tutorial path, and the Scepter's real identity replacing the stick placeholder.

## Design decisions

- **Command tree** (player commands need no permission; admin subtree stays permission level 2):

  | Command | Purpose |
  |---|---|
  | `/empiresmc status` | Used/total chunks, tier, next-tier cost |
  | `/empiresmc claims` | List claimed chunks with dimension + coordinates |
  | `/empiresmc help` | Gesture cheat-sheet |
  | `/empiresmc admin claiminfo / profile / scepter / reload` | Phase 2/3/7 tools, consolidated |
  | `/empiresmc admin settier <player> <n>` / `grantclaims <player> <n>` / `forceunclaim` | Balance-testing and rescue tools |

- **Advancement tab as the tutorial:** a small chain (receive the Scepter → first claim → first denial seen → first upgrade → max tier) teaches the loop natively; the Phase 4 join message stays as the only push notification. Advancements are data files — no new systems.
- **Real Scepter art:** custom item model/texture replacing the stick, ideally with subtle per-tier visual variation (model predicate off synced tier). Art is allowed to be simple; distinct silhouette is the bar.
- **Audio identity:** distinct sounds for claim, unclaim, upgrade, and denial (vanilla sound events tuned by pitch — custom sounds are backlog). **Every one of these must be played through `util/PlayerSounds.playTo(player, sound, volume, pitch)`, never `player.playSound(...)`** — the latter, called server-side, plays to everyone *except* that player (`ServerLevel` turns the entity argument into `PlayerList.broadcast`'s exclusion argument), so in single-player it is silently inaudible. That bug shipped in Phase 4 and went unnoticed until the Phase 5 playtest; the seam exists specifically so this phase's pitch-tuning work can't reintroduce it. See the [Phase 5 doc](phase-5-protection-enforcement.md) for the full root cause.
- **Optional, decide-in-phase:** ModMenu + Cloth Config screen for the Phase 7 file. Two new dependencies (constitution `SEC-005` — flag for review); if skipped, the JSON file + README reference remain the supported path. Lean toward skipping for 1.0 unless it's trivial.

## Steps

- [ ] Implement the command tree (Brigadier via Fabric API), all output translatable, admin subtree gated at permission level 2.
- [ ] Full localization audit: every user-facing string through `en_us.json`; in-dev sweep for raw translation keys (fabric `DEV-013` — missing keys render silently as raw keys, so grep + eyeball every screen/message).
- [ ] Advancement chain data files with fitting flavor text.
- [ ] Scepter model/texture (+ per-tier predicate if cheap); update `README` screenshots later in Phase 11.
- [ ] Sound pass on all four core actions + denial throttle interplay (Phase 5) re-checked. Must be verified **by ear in a real client** — no gametest can observe it (see Risks below) — and every call site routed through `util/PlayerSounds`; grep for `playSound(` to confirm none crept back in.
- [ ] Decide and record the ModMenu/Cloth question; implement only if accepted.
- [ ] Gametests: command outputs for status/claims match `ClaimService` truth; settier/grantclaims mutate correctly; forceunclaim bypasses cooldown but still refunds per config.

## Risks & flags

- **This phase's two headline deliverables both fail *silently* and invisibly to CI.** A sound that never reaches the client throws nothing and passes every gametest (Phase 4's sounds were dead for two phases before anyone heard the silence); a missing translation key renders as the raw key with no error (fabric `DEV-013`). Gametests run headless and server-side, so neither is automatable — budget real manual client time for the localization sweep and the sound pass rather than trusting a green build, and treat "CI green" as saying nothing about either.
- Command names/UX are API — renaming after 1.0 breaks muscle memory and any tutorials/videos; settle names in this phase deliberately.
- The advancement chain must not gate anything mechanical (pure tutorial) — gating claims behind advancements is a backlog idea for a "guided mode", not 1.0.
- Per-tier model predicates need the Phase 6 synced tier on the client — if the predicate plumbing fights back, ship one good model and move on; polish must not stall hardening.

## Exit criteria

- A player who has never read the README can install the mod, follow the advancement/messages, and reach their second tier unaided (hallway-test this on one person if possible).
- No raw translation keys anywhere; commands behave per table; CI green.
