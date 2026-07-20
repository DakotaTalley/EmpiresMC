# Phase 4 — Claiming & unclaiming

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 3](phase-3-the-scepter.md). Next: [Phase 5 — Protection enforcement](phase-5-protection-enforcement.md).

## Goal

The full claim lifecycle is playable with the Scepter: place the starting claim, expand when allowance permits, inspect status, and unclaim behind a cooldown.

## Design decisions

- **Claims target the chunk the player is standing in**, not the chunk of the block they're looking at — unambiguous, works mid-air and underground, and Phase 6's preview outline will make the target visible. (Raycast-targeted claiming is a backlog idea if standing-chunk feels bad in playtesting.)
- **Gesture map** (all resolved on the logical server; the client only sends the vanilla use action — fabric `SEC-001`):

  | Context | Gesture | Result |
  |---|---|---|
  | Unclaimed chunk | use | Claim it (if allowance remains) |
  | Own claimed chunk | use | Status: chunk pos, used/total chunks, tier, cooldown state |
  | Own claimed chunk | sneak-use | Unclaim — two-step: first use shows a confirm prompt, second use within ~5s commits |
  | Anywhere else | sneak-use | Reserved for the Phase 8 upgrade flow |

- **Unclaim cooldown runs on world game time** (`claimedAtTick` from Phase 2), not wall-clock — wall-clock time passes while the game is closed, which would make the cooldown meaningless in single-player. Dev default: 1 in-game day (24,000 ticks) per chunk from the moment it was claimed; configurable in Phase 7.
- **Unclaim refunds the full allowance slot** for now. This is a known balance lever — see the exploit flag below and Phase 10.
- **No adjacency requirement in the MVP.** Free-floating claims keep the Nether reachable (portal → claim a Nether chunk to mine) without special-casing dimensions. `requireAdjacency` becomes a config option in Phase 7 and a Phase 10 balance question.
- **All dimensions claimable, one shared allowance.** Simple and consistent; the Nether-progression consequences are a Phase 10 balance checkpoint (netherite is a Phase 8 upgrade cost, so Nether mining must be affordable within the curve).
- The first claim is a deliberate act: on first join (Phase 3 grant moment) the player gets a short instruction message — no auto-claimed spawn chunk.

## Steps

- [ ] Implement the Scepter `use` path: resolve the standing chunk server-side, dispatch to `ClaimService`, map each typed result to a distinct feedback message + sound (claim success, no allowance, already yours, owned-by-other for future MP).
- [ ] Implement sneak-use unclaim with the two-step confirm (pending-confirm state per player, ~100-tick expiry) and cooldown gate; denial message includes remaining cooldown in readable form (e.g. "0.4 days").
- [ ] Debounce repeated use events (vanilla fires use on both hands / repeatedly while held): ignore duplicate gestures within a few ticks so one click is one action.
- [ ] First-join instruction message ("Use your Scepter in a chunk to claim your first land") wired into the Phase 3 grant.
- [ ] All feedback via translatable components in `en_us.json` (fabric `DEV-013`).
- [ ] Gametests: claim happy path; allowance exhaustion at 1 chunk; status gesture; unclaim blocked before cooldown; unclaim succeeds after cooldown (fast-forward world time); confirm-expiry cancels the unclaim; refunded slot is claimable elsewhere.

## Risks & flags

- **Exploit — nomadic strip-mining:** claim a chunk, exhaust its resources, wait out the cooldown, unclaim, claim virgin land, repeat. The cooldown throttles but does not close this; a full close needs a refund penalty (`unclaimRefundPercent < 100`) or per-chunk "scarring". Deliberately deferred to Phase 10 balance with config knobs from Phase 7 — do not try to solve it in this phase.
- **Exploit — time skipping:** sleeping and `/time set` advance world time, shortening the cooldown. Accepted for 1.0: single-player self-cheating via commands is out of scope, and sleep-skipping costs a night either way. Documented so it's a conscious acceptance, not an oversight.
- **Gesture collision risk:** sneak-use means "unclaim" inside a claim and "upgrade" (Phase 8) outside it — a player standing one step wrong could trigger the wrong flow. Both flows therefore require a confirm step, and both print which chunk/action they're about to commit. If playtesting still shows misfires, a management GUI (backlog) replaces gestures.
- Claiming currently has no cost beyond allowance — the "cost" of expansion is entirely the Phase 8 upgrade ladder. If playtesting wants per-claim costs too (e.g. consume an item per chunk), that's a new config knob + phase, not a rework, because `ClaimService` already returns typed denials.

## Exit criteria

- All Phase 4 gametests green in CI.
- Manual dev run: fresh world → guided by messages alone, a player claims their starting chunk, is denied a second, and can unclaim/reclaim after the cooldown — with every message localized.
