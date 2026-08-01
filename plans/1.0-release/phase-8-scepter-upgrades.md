# Phase 8 — Scepter upgrades & claim costs

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 7](phase-7-configuration.md). Next: [Phase 9 — Commands & polish](phase-9-commands-and-polish.md).

## Goal

The progression engine: the Scepter upgrades through config-defined tiers, each costing increasingly rare resources plus experience and granting additional claimable chunks — and each individual claim costs an item, so expansion is paid for twice over.

**Scope added mid-plan** (from the [market research](../research/market_research.md) review): the per-claim item cost, promoted out of the backlog to become 1.0's anti-churn mechanism in place of the `unclaimRefundPercent` idea Phase 7 originally reserved. It lands here rather than in its own phase because it is the same problem as an upgrade cost — verify affordability from the main inventory, report what's missing, consume atomically — and should share one service rather than growing a second copy of that logic. See [Phase 7](phase-7-configuration.md) for the reasoning behind item-cost-over-refund-percentage.

## Design decisions

- **The tier ladder lives in the config file, not in code and not in datapack recipes.** The project brief's top requirement here is cheap balance iteration — editing JSON and running `/empiresmc admin reload` must be the whole loop. (A datapack-driven alternative for modpack authors is a backlog idea; the config schema doesn't preclude it.)
  - **Read the ladder through a single resolution point** — one function that answers "what is the tier table right now" — rather than reading the config object directly at each call site. Adding the backlog's datapack source later then means adding an input to one function instead of touching every reader and reasoning about precedence retroactively. Costs nothing now; it's the only part of that backlog idea with a deadline.
- **Tier schema:** `tiers: [ { items: [ { id, count } ], xpLevels, chunksGranted } ]` — index in the list is the tier number; the player's `scepterTier` (Phase 2) indexes into it. Allowance = `startingClaims` + sum of `chunksGranted` up to current tier — still derived, never stored.
- **Costs use XP *levels*, not points** — levels are what the player sees on screen, making costs legible without math.
- **Upgrade is a dedicated keybind** (revised 2026-08-01; the original design used sneak-use outside your own claims, which Phase 4 reserved and playtesting then rejected along with the rest of the crouch mechanic). It reuses the keybind + hold-to-confirm mechanism [Phase 6](phase-6-claim-visualization.md) builds, so this phase adds an action to existing plumbing rather than a new input model: press to see the next tier's full cost and what's missing, hold to consume and upgrade. Notably the upgrade no longer depends on *where the player stands* — the old gesture only worked outside your own claims, purely to avoid colliding with unclaim, which was never meaningful to the player. A management GUI is still a backlog item; the keybind must be fully functional regardless.
- **Consumption is atomic and server-side:** verify full affordability (items across main inventory + XP levels), then remove everything in one step. Never partially consume. Shulker boxes and ender chests are *not* searched — main inventory only, stated in the cost message.
- **Creative mode upgrades free** (consistent with Phase 5's creative bypass; makes balance testing the tier curve trivial).
- **Per-claim cost is `claimCostItems` (Phase 7 schema), charged on claim and never refunded on unclaim.** Unclaiming still returns the allowance slot in full — the cost of churn is paying the item again to reclaim. Draft default is a small quantity of a mid-common material (tuned in Phase 10 alongside the tier ladder); the same atomic check-then-consume discipline as an upgrade applies, and the denial reports the itemized shortfall exactly as the upgrade flow does.
- **Waived while the player owns zero chunks** (`waiveClaimCostWhenLandless`, default true) — the soft-lock guard, since a landless player has no legal way to gather anything ([Phase 5](phase-5-protection-enforcement.md) forbids breaking in the wild). Practical effect: tier 1 never pays, so the cost is a tier-2-and-up mechanic and the first-run experience is unchanged from Phase 4's.
- **Draft default ladder — explicitly placeholder numbers, tuned in Phase 10.** The intended shape: early tiers from common ores, mid tiers push exploration, late tiers demand Nether and boss content, and per-tier chunk grants grow so late upgrades feel generous.

  | Tier | Cost (draft) | Grants | Cumulative chunks |
  |---|---|---|---|
  | 1 (start) | — | 1 | 1 |
  | 2 | 16 iron ingot, 3 levels | +3 | 4 |
  | 3 | 8 gold ingot + 16 redstone, 5 levels | +4 | 8 |
  | 4 | 4 diamond, 8 levels | +5 | 13 |
  | 5 | 16 obsidian + 4 ender pearl, 12 levels | +6 | 19 |
  | 6 | 1 netherite ingot, 16 levels | +8 | 27 |
  | 7 | 1 nether star, 20 levels | +12 | 39 |

## Steps

- [ ] Extend the Phase 7 config schema with the `tiers` list + the draft defaults above; validate item ids at server start (warn and disable the affected tier on unknown ids — modpack-safe, never crash).
- [ ] Implement `UpgradeService`: affordability check with itemized missing-cost report, atomic consume (items + levels), tier increment, allowance recalculation. Factor the affordability/consume half so the claim-cost path reuses it rather than reimplementing it.
- [ ] Wire the per-claim cost into `ClaimService.claim`: check `waiveClaimCostWhenLandless` first, then affordability, then consume atomically *after* the claim is known to succeed (never consume on an allowance denial). Needs a new `ClaimResult.CannotAfford(missing)` variant — Phase 4 anticipated exactly this ("`ClaimService`'s typed results already leave room"). Creative bypasses the cost, matching the creative-free upgrade rule above.
- [ ] Wire the upgrade keybind onto Phase 6's action-payload + hold-to-confirm mechanism; at max tier, report "fully upgraded" instead of a cost.
- [ ] Feedback: cost/missing message uses item display names; success plays a level-up-style sound (via `util/PlayerSounds.playTo` — `player.playSound(...)` server-side is inaudible to the acting player, see [Phase 5](phase-5-protection-enforcement.md)) + message; Phase 6 delta payload carries the tier change so tooltip/HUD update instantly.
- [ ] Localize all upgrade messages (`en_us.json`).
- [ ] Unit tests: allowance derivation across tiers, affordability edge cases (exact amounts, split stacks, levels-but-no-items and vice versa), tier-table-shrunk-below-current-tier handling (treat as max tier; never crash or revoke).
- [ ] Gametests: successful upgrade consumes exactly the configured cost and immediately permits claiming the granted chunks; denied upgrade consumes nothing; confirm expiry; creative free upgrade. For claim costs: claiming consumes exactly the cost; an unaffordable claim consumes nothing *and* does not claim; an allowance-denied claim consumes nothing; a landless player claims free; unclaim refunds the slot but not the item.

## Risks & flags

- **Exploit — consumption gaps:** any path where the check and the consume see different inventory (moving items during the confirm window) must fail closed — re-verify at consume time inside the same server-tick action. The "consumes nothing on denial" gametest guards it.
- **Balance risk — netherite tier vs. Nether claiming:** tier 6 requires Nether mining, which requires spending claims in the Nether under the shared allowance. The curve must leave slack for that by tier 5 — explicit Phase 10 playtest checkpoint.
- Editing the tier table mid-save (fewer tiers, changed grants) recomputes allowances retroactively since allowance is derived; players can end up over-allowance, which per Phase 7's rule blocks new claims but never revokes. Test it.
- No downgrade/respec in 1.0 — flagged as accepted scope; backlog has a refund-respec idea if playtesting wants it.

## Exit criteria

- Full progression playable in dev: start at 1 chunk, upgrade through every default tier by gathering costs (or creative), ending at 39 claimable chunks, with the claim/deny/confirm loop readable throughout.
- Editing a tier cost in the config + reload changes the quoted cost without restart.
- All tests green in CI.
