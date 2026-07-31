# Phase 8 — Scepter upgrades

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 7](phase-7-configuration.md). Next: [Phase 9 — Commands & polish](phase-9-commands-and-polish.md).

## Goal

The progression engine: the Scepter upgrades through config-defined tiers, each costing increasingly rare resources plus experience and granting additional claimable chunks.

## Design decisions

- **The tier ladder lives in the config file, not in code and not in datapack recipes.** The project brief's top requirement here is cheap balance iteration — editing JSON and running `/empiresmc admin reload` must be the whole loop. (A datapack-driven alternative for modpack authors is a backlog idea; the config schema doesn't preclude it.)
- **Tier schema:** `tiers: [ { items: [ { id, count } ], xpLevels, chunksGranted } ]` — index in the list is the tier number; the player's `scepterTier` (Phase 2) indexes into it. Allowance = `startingClaims` + sum of `chunksGranted` up to current tier — still derived, never stored.
- **Costs use XP *levels*, not points** — levels are what the player sees on screen, making costs legible without math.
- **Upgrade gesture: sneak-use outside own claims** (the slot Phase 4 reserved), with the same two-step confirm: first use prints the next tier's full cost and what's missing; second use within ~5s consumes and upgrades. A management GUI that supersedes gestures is a backlog item — gestures must be fully functional regardless.
- **Consumption is atomic and server-side:** verify full affordability (items across main inventory + XP levels), then remove everything in one step. Never partially consume. Shulker boxes and ender chests are *not* searched — main inventory only, stated in the cost message.
- **Creative mode upgrades free** (consistent with Phase 5's creative bypass; makes balance testing the tier curve trivial).
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
- [ ] Implement `UpgradeService`: affordability check with itemized missing-cost report, atomic consume (items + levels), tier increment, allowance recalculation.
- [ ] Wire the sneak-use gesture with two-step confirm; at max tier, report "fully upgraded" instead of a cost.
- [ ] Feedback: cost/missing message uses item display names; success plays a level-up-style sound (via `util/PlayerSounds.playTo` — `player.playSound(...)` server-side is inaudible to the acting player, see [Phase 5](phase-5-protection-enforcement.md)) + message; Phase 6 delta payload carries the tier change so tooltip/HUD update instantly.
- [ ] Localize all upgrade messages (`en_us.json`).
- [ ] Unit tests: allowance derivation across tiers, affordability edge cases (exact amounts, split stacks, levels-but-no-items and vice versa), tier-table-shrunk-below-current-tier handling (treat as max tier; never crash or revoke).
- [ ] Gametests: successful upgrade consumes exactly the configured cost and immediately permits claiming the granted chunks; denied upgrade consumes nothing; confirm expiry; creative free upgrade.

## Risks & flags

- **Exploit — consumption gaps:** any path where the check and the consume see different inventory (moving items during the confirm window) must fail closed — re-verify at consume time inside the same server-tick action. The "consumes nothing on denial" gametest guards it.
- **Balance risk — netherite tier vs. Nether claiming:** tier 6 requires Nether mining, which requires spending claims in the Nether under the shared allowance. The curve must leave slack for that by tier 5 — explicit Phase 10 playtest checkpoint.
- Editing the tier table mid-save (fewer tiers, changed grants) recomputes allowances retroactively since allowance is derived; players can end up over-allowance, which per Phase 7's rule blocks new claims but never revokes. Test it.
- No downgrade/respec in 1.0 — flagged as accepted scope; backlog has a refund-respec idea if playtesting wants it.

## Exit criteria

- Full progression playable in dev: start at 1 chunk, upgrade through every default tier by gathering costs (or creative), ending at 39 claimable chunks, with the claim/deny/confirm loop readable throughout.
- Editing a tier cost in the config + reload changes the quoted cost without restart.
- All tests green in CI.
