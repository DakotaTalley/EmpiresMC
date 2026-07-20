# Phase 3 — The Scepter

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 2](phase-2-claim-data-and-persistence.md). Next: [Phase 4 — Claiming & unclaiming](phase-4-claiming-and-unclaiming.md).

## Goal

The Scepter exists as a registered item the player receives on first join and can never permanently lose — without any gameplay-inhibiting restrictions like "cannot drop".

## Design decisions

- **The Scepter is a stateless handle.** No tier, no claim data, no owner stored on the `ItemStack` — all of that lives in the Phase 2 server-side profile keyed by the *holder's* UUID. This one decision is the whole loss-protection strategy:
  - Losing the item never loses progress; any replacement Scepter reflects the same empire.
  - Duplication (creative middle-click, inventory tricks) is harmless — two Scepters are two handles to the same state.
  - Recovery can be a cheap crafting recipe with no "is this the real one" logic.
  - In any future multiplayer mode, whoever holds *a* scepter acts on *their own* empire — no theft or transfer semantics needed.
- **Layered protection, no gameplay friction** (the item stays droppable, storable, hopper-able):
  1. Keep on death — the Scepter stays in the inventory through death instead of dropping.
  2. Dropped-item hardening — the item entity never despawns and is immune to fire/lava (netherite-style fireproof settings + item-entity hooks) and explosions.
  3. Recovery of last resort — a cheap, always-available crafting recipe (dev placeholder: stick + iron ingot, shapeless) plus `/empiresmc admin scepter`. Void loss is therefore acceptable rather than needing a teleport-back hack.
- **Dev-placeholder visuals per the project brief:** stick model/icon, item named "Scepter", rarity Epic so the name reads as special. Real art lands in Phase 9.
- Item settings: `maxCount = 1` (no stacking; keeps the tooltip/HUD semantics simple).

## Steps

- [ ] Register `ScepterItem` in the Phase 1 registry holder: fireproof-style settings, max count 1, Epic rarity; stick model + `item.empiresmc.scepter` translation key (fabric `DEV-013`: a missing lang entry shows the raw key — add `en_us.json` now).
- [ ] First-join grant: on player join, if the profile's `receivedScepter` flag is unset, give one Scepter and set the flag (flag prevents re-grant spam; recovery goes through the recipe instead).
- [ ] Keep-on-death: hook the death drop path (mixin into the inventory-drop routine or the closest Fabric API event in the pinned version) to retain the Scepter through respawn.
- [ ] Dropped-entity hardening: unlimited item-entity lifetime, fire/lava and explosion immunity for Scepter item entities.
- [ ] Recovery recipe (data-generated or JSON under `data/empiresmc/recipe/`): cheap, unconditional. Harmless to craft twice by design.
- [ ] Static tooltip: flavor line + "used / total chunks shown while claim data is synced" placeholder (live numbers arrive with Phase 6 client sync).
- [ ] Tests: unit test the grant-flag logic; gametest that a fresh player receives exactly one Scepter and a rejoin grants none.

## Risks & flags

- **Do not add per-stack NBT "convenience" state later** (e.g. caching tier on the stack for tooltips) without treating it as pure display cache — the moment stack NBT becomes authoritative, every dupe/loss protection above collapses. Phase 6's sync layer is the sanctioned way to get live client-side numbers.
- Keep-on-death interacts with other mods' grave/corpse mods — out of scope for 1.0, note in README known-issues at release.
- The recovery recipe means the Scepter is craftable before first join edge-cases (multiplayer/LAN) — harmless by design, but worth a test once multiplayer is ever targeted.
- `keepInventory` gamerule already covers death; the custom keep-on-death path must not duplicate the item when both are active — add a gametest for this combination.

## Exit criteria

- Fresh world: player spawns holding nothing, receives the Scepter on join, message/tooltip render with no raw translation keys.
- Scepter survives: player death (with and without `keepInventory`), being dropped into lava, a nearby TNT blast, and a 6000+ tick wait on the ground.
- Losing it into the void, then crafting the recovery recipe, yields a working Scepter.
