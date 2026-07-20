# Phase 5 — Protection enforcement

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 4](phase-4-claiming-and-unclaiming.md). Next: [Phase 6 — Claim visualization](phase-6-claim-visualization.md).

## Goal

The core rule of the mod is enforced: outside their claims the player cannot change the world — no breaking, no placing, no block-modifying item use — while ordinary interaction (doors, chests, looting structures) still works.

## Design decisions

- **The restriction model is inverted from classic claim mods.** Claims don't protect land *from others*; they define the only places the *player* may alter. Consequences that shape this phase:
  - Non-block gameplay stays free everywhere: movement, combat, item pickup, trading, sleeping.
  - "Pure" interactions (open door/chest, press button, use crafting table, loot generated structures) are **allowed** in the wild — exploration and looting are the intended counterweight to restricted mining. A strict-mode config toggle can come later; it is not MVP.
  - "Modifying" interactions (bucket place/drain, flint & steel, hoe till, shovel path, axe strip, bone meal) are **denied** in the wild — each is a block change wearing an interaction costume.
- **Explosion policy: player-independent simplicity.** No explosion may destroy blocks in unclaimed chunks (block damage filtered per-position; entity damage untouched). Rationale: TNT-mining the wild would bypass the entire claim economy (see flags), attributing explosion causes is fragile, and "the wild is not yours to alter — by any means" is thematically coherent. Creeper craters not appearing in the wild is an accepted, even desirable, side effect. Config toggle in Phase 7.
- **Creative mode bypasses all restrictions** (standard modding convention, and it keeps dev/testing fast). Survival/adventure enforce.
- **Denial feedback must not spam:** action-bar message + soft sound, throttled per player (~1/second), since holding left-click fires break attempts every tick.
- Implementation surfaces (exact APIs confirmed against the pinned Fabric API version at implementation time):
  - Break: `PlayerBlockBreakEvents.BEFORE` (authoritative cancel) + `AttackBlockCallback` (early client+server deny so blocks don't even crack).
  - Place: Fabric API block-place event if the pinned version has one; otherwise a small mixin into the `BlockItem` placement path checking the *placement* position's chunk (not the clicked block's — placing across a chunk border must check where the block lands).
  - Modifying interactions: `UseBlockCallback`/`UseItemCallback` filters keyed on a deny-list of modifying behaviors.
  - Explosions: hook the explosion block-damage list and strip unclaimed positions.

## Steps

- [ ] Central `ProtectionService.canModify(player, world, pos): Boolean` (+ deny reason) so every hook shares one decision path — rules live in one place, hooks stay dumb.
- [ ] Wire break denial (both events), with the crack-prevention path verified.
- [ ] Wire place denial via the placement-position chunk check, including multi-block placements (doors, beds: check both halves).
- [ ] Wire modifying-interaction denial: buckets (place and pickup), flint & steel, bone meal, hoe/shovel/axe right-click transforms.
- [ ] Wire the explosion block-damage filter (TNT, creeper, bed/respawn-anchor wrong-dimension explosions all flow through it).
- [ ] Throttled action-bar denial feedback with translatable messages.
- [ ] Creative bypass + a shared "enforcement enabled" predicate (single seam for the Phase 7 config toggles).
- [ ] Gametests: break denied in wild / allowed in claim; place denied in wild / allowed in claim; cross-border placement denied; bucket denied in wild; bone meal denied in wild; chest/door interaction *allowed* in wild; TNT detonated in wild leaves blocks intact; TNT in own claim breaks blocks; creative bypass works.

## Risks & flags

Flags that stay open here and land in the [Phase 10 exploit register](phase-10-exploit-hardening-and-balance.md):

- **Exploit (closed this phase, verify hard): TNT/creeper mining the wild.** Without the explosion filter, explosives harvest unclaimed resources and break the whole economy. The gametest for this is non-negotiable.
- **Exploit (open): machines reaching across the border.** Pistons/slime contraptions inside a claim pushing, pulling, or breaking blocks in the wild; dispensers placing/breaking at the border. Not addressed in MVP — single-player self-cheating — but registered for Phase 10 (likely a config-gated border check on piston/dispenser action).
- **Exploit (open): indirect world edits.** Water/lava poured in-claim flowing into the wild (can break blocks, make obsidian), falling sand/gravel crossing the border, fire spreading from an in-claim ignition, tree growth / bone-mealed growth spilling across the border. All registered for Phase 10 dispositions.
- **Balance risk (major, intentional): early-game wood and food.** Trees, grass, and crops in the wild are unbreakable, so a bad first-claim placement can soft-lock progression comfort. Options for Phase 10: a config allow-list of wild-breakable blocks (plants/leaves), or doubling down (guides players to claim wisely; loot/trade for food). MVP ships strict; playtesting decides.
- Entity-based "building" (item frames, armor stands, boats, minecarts, paintings, leads) is not block placement and stays unrestricted in MVP — registered for Phase 10.
- Farmland trampling and frost-walker ice are technically wild block changes — trivial, registered for Phase 10, likely "allow".

## Exit criteria

- Full gametest suite above green in CI.
- Manual dev run: with one claimed chunk, the world is only alterable inside it — mining, building, buckets, bone meal, TNT all behave per the table above, with readable feedback and no message spam.
