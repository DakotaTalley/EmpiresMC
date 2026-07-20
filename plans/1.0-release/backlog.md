# Backlog — unscheduled ideas

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Nothing here is committed. When an idea is scheduled, it becomes a numbered phase file plus a row in the summary — that's the plan's extension mechanism, and it works mid-development at any time.

## Gameplay

- **Claim perks:** buffs while inside your own territory (minor regen/haste tiers) — makes claimed land feel *owned*, not just permitted.
- **Per-claim costs** in addition to upgrade-gated allowance (consume an item per chunk claimed) — extra economy lever; `ClaimService`'s typed results already leave room.
- **Chunk scarring / decay:** unclaimed-after-use chunks remember exploitation — the strongest fix for nomadic strip-mining (register #1) if refund-percent proves insufficient.
- **Guided mode:** advancement-gated tier unlocks (tiers require progression milestones, not just resources).
- **Raycast claim targeting** (claim the chunk you're looking at) if standing-chunk targeting tests poorly.
- **Downgrade/respec** with partial refund.
- **Structure-aware claiming rules** (e.g. can't claim village chunks, or claiming them has consequences).

## Multiplayer (data model is ready; everything else isn't)

- LAN/server support as a first-class mode: per-player empires already keyed by UUID (Phase 2).
- Teams/shared empires, ally permissions.
- Claim protection *from other players* — the classic direction, inverse of 1.0's model.

## UX & integration

- Management GUI replacing sneak-use gestures (claim map, unclaim buttons, upgrade screen).
- ModMenu + Cloth Config settings screen (if declined in Phase 9).
- Map-mod integration: claim overlays for Xaero's/JourneyMap.
- Custom sound set replacing pitched vanilla sounds.
- Additional languages beyond `en_us` (community translations post-publish).
- Config format upgrade to TOML/JSON5 for comments (`configVersion` makes it mechanical).

## Technical

- Datapack-driven tier definitions for modpack authors (alongside, not replacing, config tiers).
- Loom split source sets if client/server code separation gets hairy (fabric `DEV-008`).
- Fluid-flow and full indirect-edit tracking (register #6/#7) if players demand airtight wild protection.
- Minecraft version ports (policy set in Phase 1: port after 1.0, not during).
