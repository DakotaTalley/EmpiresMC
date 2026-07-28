# Phase 5 — Protection enforcement

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 4](phase-4-claiming-and-unclaiming.md). Next: [Phase 6 — Claim visualization](phase-6-claim-visualization.md).

## Goal

The core rule of the mod is enforced: outside their claims the player cannot change the world — no breaking, no placing, no block-modifying item use — while ordinary interaction (doors, chests, looting structures) still works.

## Design decisions

- **The restriction model is inverted from classic claim mods.** Claims don't protect land *from others*; they define the only places the *player* may alter. Consequences that shape this phase:
  - Non-block gameplay stays free everywhere: movement, combat, item pickup, trading, sleeping.
  - "Pure" interactions (open door/chest, press button, use crafting table, loot generated structures) are **allowed** in the wild — exploration and looting are the intended counterweight to restricted mining. A strict-mode config toggle can come later; it is not MVP.
  - "Modifying" interactions (bucket place/drain, flint & steel, hoe till, shovel path, axe strip, bone meal) are **denied** in the wild — each is a block change wearing an interaction costume.
- **Explosion policy: left as vanilla, by decision.** Explosions behave identically in claims and the wild — no block-damage filtering. No Fabric API event exists for this, and the only interception points (filtering the explosion's block-damage list, or restricting pickup of the resulting drops) both require a mixin; rather than add mixin infrastructure for it, the call is to accept vanilla explosion behavior for 1.0. Rationale: world-destructive explosions read as consistent with vanilla Minecraft, and TNT/creeper mining the wild takes real setup and skill (positioning explosives, luring creepers) — a meaningfully higher bar than passive strip-mining. This reopens exploit-register item #3 in the [Phase 10 exploit register](phase-10-exploit-hardening-and-balance.md) as accepted/monitored rather than closed; revisit if the balance playthrough shows it's exploited more heavily than expected.
- **Creative mode bypasses all restrictions** (standard modding convention, and it keeps dev/testing fast). Survival/adventure enforce.
- **Denial feedback must not spam:** action-bar message + soft sound, throttled per player (~1/second), since holding left-click fires break attempts every tick.
- Implementation surfaces (confirmed against the pinned Fabric API version by decompiling the actual jars):
  - Break: `PlayerBlockBreakEvents.BEFORE` (authoritative cancel) + `AttackBlockCallback` (early client+server deny so blocks don't even crack).
  - Place: no mixin needed — `UseBlockCallback` fires at the head of `ServerPlayerGameMode.useItemOn`, before vanilla's own block-use, modifying-interaction, and placement dispatch, so canceling it there pre-empts placement too. Denial checks the *placement* position's chunk (built via a real `BlockPlaceContext`, not the clicked block's) — placing across a chunk border must check where the block lands, including a multi-block placement's companion half (beds; doors can't cross a chunk border, their second half is a pure vertical offset).
  - Modifying interactions: the same `UseBlockCallback` hook, filtered on a deny-list of modifying item classes (bucket, flint & steel, hoe, shovel, axe, bone meal).
  - Central decision logic is split into three independently gate-able predicates — `canBreak`/`canPlace`/`canInteract` — rather than one shared check, so a future Phase 7 config can enable/disable break, placement, and modifying-item-use enforcement independently instead of all-or-nothing.

## Steps

- [ ] Central `ProtectionService.canBreak`/`canPlace`/`canInteract(player, world, pos): ProtectionResult` (+ deny reason: wild vs. owned by someone else) so every hook shares one decision path per action kind — rules live in one place, hooks stay dumb, and each action kind is independently gate-able for Phase 7.
- [ ] Wire break denial (both events), with the crack-prevention path verified.
- [ ] Wire place denial via the placement-position chunk check, including multi-block placements (beds: check both halves; doors need no equivalent — their second half is a pure vertical offset and can't cross a chunk border).
- [ ] Wire modifying-interaction denial: buckets (place and pickup), flint & steel, bone meal, hoe/shovel/axe right-click transforms.
- [ ] Throttled action-bar denial feedback with translatable messages, distinguishing wild from owned-by-someone-else.
- [ ] Creative bypass, applied identically across `canBreak`/`canPlace`/`canInteract` (single seam per predicate for the Phase 7 config toggles).
- [ ] Gametests: break denied in wild / on someone else's claim / allowed in own claim; place denied in wild / allowed in claim; cross-border placement denied; bucket denied in wild; bone meal denied in wild; chest/door interaction *allowed* in wild; creative bypass works.

## Risks & flags

Flags that stay open here and land in the [Phase 10 exploit register](phase-10-exploit-hardening-and-balance.md):

- **Exploit (accepted, monitored): TNT/creeper mining the wild.** By decision, not oversight — explosions are left as vanilla behavior for 1.0 (see the design decision above). Registered as exploit-register item #3 in Phase 10 with disposition "allow (documented)"; revisit if the balance playthrough shows it's exploited more heavily than the setup/skill cost suggests it should be.
- **Exploit (open): machines reaching across the border.** Pistons/slime contraptions inside a claim pushing, pulling, or breaking blocks in the wild; dispensers placing/breaking at the border. Not addressed in MVP — single-player self-cheating — but registered for Phase 10 (likely a config-gated border check on piston/dispenser action).
- **Exploit (open): indirect world edits.** Water/lava poured in-claim flowing into the wild (can break blocks, make obsidian), falling sand/gravel crossing the border, fire spreading from an in-claim ignition, tree growth / bone-mealed growth spilling across the border. All registered for Phase 10 dispositions.
- **Balance risk (major, intentional): early-game wood and food.** Trees, grass, and crops in the wild are unbreakable, so a bad first-claim placement can soft-lock progression comfort. Options for Phase 10: a config allow-list of wild-breakable blocks (plants/leaves), or doubling down (guides players to claim wisely; loot/trade for food). MVP ships strict; playtesting decides.
- Entity-based "building" (item frames, armor stands, boats, minecarts, paintings, leads) is not block placement and stays unrestricted in MVP — registered for Phase 10.
- Farmland trampling and frost-walker ice are technically wild block changes — trivial, registered for Phase 10, likely "allow".

## Exit criteria

- Full gametest suite above green in CI.
- Manual dev run: with one claimed chunk, the world is only alterable inside it — mining, building, buckets, bone meal all behave per the table above, with readable feedback and no message spam. Explosions behave identically in and out of claims (unmodified vanilla — intentional for 1.0).
