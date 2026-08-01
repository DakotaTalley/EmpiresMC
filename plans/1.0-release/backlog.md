# Backlog — unscheduled ideas

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Nothing here is committed. When an idea is scheduled, it becomes a numbered phase file plus a row in the summary — that's the plan's extension mechanism, and it works mid-development at any time.

## Gameplay

- **Claim perks:** buffs while inside your own territory (minor regen/haste tiers) — makes claimed land feel *owned*, not just permitted.
- ~~**Per-claim costs**~~ — **promoted into [Phase 8](phase-8-scepter-upgrades.md)** by the [market research](../research/market_research.md) review: it became 1.0's anti-churn mechanism in place of `unclaimRefundPercent`, which Phase 2's derived-allowance model can't represent.
- **Chunk scarring / decay:** unclaimed-after-use chunks remember exploitation. The research rates this the mod's **only genuinely novel mechanic** — no precedent found in any mod, plugin, or datapack — and recommends it as the flagship, heavily-marketed 1.x feature.
  - **This is the better-targeted answer to nomadic strip-mining (register #1), not a fallback if the per-claim cost fails.** The two fixes differ in kind: the [Phase 8](phase-8-scepter-upgrades.md) per-claim item cost taxes *every* claim, so honest first-time expansion pays exactly what abusive reclaiming pays, while scarring taxes only the reuse of exploited ground. Scarring is deferred on **schedule grounds** — the mechanic is undefined and would ship unplaytested — not because it's the weaker mechanism. Reviewed and re-confirmed 2026-08-01.
  - **What's undefined, and must be settled before it's scheduled:** what scarring actually *does* (reduced yield / higher reclaim cost / permanent ineligibility / slower regrowth / visual decay only) — each has a different feel and a different cost, and the research itself recommends a config to disable it for players who find it punishing.
  - **It needs per-chunk state that outlives the claim**, which Phase 2 does not store, and a nomadic player generates those records without bound — so it needs a cap or a decay on the decay. The interesting variants also key on *exploitation* (blocks broken, ore extracted), meaning instrumentation on the hot break path.
  - **Scarring will apply prospectively only.** Recording unclaim history in 1.0 to avoid this was considered and rejected: minimal history (chunk, last owner, unclaimed tick) is cheap but likely insufficient for the variants worth building, and recording a guess at the wrong shape buys nothing.
  - **The swap trigger lives in [Phase 10](phase-10-exploit-hardening-and-balance.md)'s playthrough** — if the per-claim cost reads as a tax on normal expansion, the answer is to swap to scarring, not to tune the cost down (tuning it down weakens the anti-churn it exists for).
- **Guided mode:** advancement-gated tier unlocks (tiers require progression milestones, not just resources).
- ~~**Raycast claim targeting**~~ — **promoted into [Phase 6](phase-6-claim-visualization.md)** on 2026-08-01: standing-chunk targeting did test poorly, so claiming is now raycast with standing-chunk fallback.
- **Empire-wide border rendering** (all owned claims within a ~5-chunk radius, outward faces only) — the original Phase 6 design, dropped from 1.0 when playtesting showed the real need was local legibility rather than empire visualization. Worth revisiting *after* map-mod integration, which serves the "see my whole empire" job better and without the in-world fill-rate cost. If it does come back, the lines-not-walls and Y-clamp constraints from Phase 6 come back with it.
- **Longer Scepter-specific raycast** for targeting chunks beyond vanilla block reach (~4.5–6 blocks), if playtesting asks for "claim that chunk over there."
- **Downgrade/respec** with partial refund.
- **Structure-aware claiming — "preservation by price".** Designed 2026-08-01 out of the [market research](../research/market_research.md) review; a 1.x candidate, deliberately not 1.0. Generated structures stay claimable, but claiming a chunk containing structure pieces costs a **premium** on top of the Phase 8 `claimCostItems`. Special places feel special, the signal is explicit and quotable, and — unlike prohibition — no configuration of bad luck can lock a save.
  - **Chunk stays the unit (Option A).** Cost lookup only: "does this chunk contain pieces from a premium-listed structure?" Nothing else changes — allowance math, the standing-chunk gesture, Phase 6 rendering, unclaim, and the confirm flow are all untouched. Match on **structure pieces, not the outer bounding box**, so the empty grass between village houses charges normal price and only chunks with actual buildings and roads pay.
  - **Option C is the playtest escape hatch:** if per-chunk premiums feel punishing on large sites, charge the premium **once per site** and normal price for subsequent chunks of the same structure, keyed on `(dimension, structure id, origin chunk pos)`. Costs new stored per-player state (the thing [Phase 2](phase-2-claim-data-and-persistence.md) avoids), so it's a response to evidence, not a starting point.
  - **Option B (whole footprint claimed at once for one price) was rejected:** a 12-chunk village exceeds a mid-tier player's entire allowance, making structures unclaimable until very late — a soft version of the soft-lock this design exists to avoid — and it breaks the "claims target the chunk you're standing in" invariant that Phase 4's gestures and Phase 6's preview outline are both built on.
  - **The landless waiver applies to the base cost only, never the premium** — otherwise a player at zero claims takes a stronghold or village chunk for free. Safe to deny, because a landless player can always walk to ordinary land, so it can't strand anyone. Recorded in [Phase 7](phase-7-configuration.md) now even though premiums don't exist yet, so the hole never opens.
  - **Full-column claims are the right call here, not a wart.** Paying the stronghold premium also buys the surface above it — which is exactly the established meta of locating a stronghold and digging down to the portal room, now with a price attached to the ground you're tunnelling through. 3D claims are not contemplated.
  - **Premiums are spent, never refunded** (unclaim returns the allowance slot but not items, per Phase 8), so churning a structure chunk means paying the premium again. Free anti-abuse on exactly the land most worth churning.
  - **No new UI needed:** Phase 8's itemized cost message already fires on the confirm step, so the first attempt reads "this chunk is part of a village — claiming costs X, you're missing Y." Phase 6 can tint the preview outline for premium chunks cheaply, since that's already a distinct render style.
  - **Config is tag-keyed** (`{ structures: "#minecraft:village", costMultiplier: N }`) so modpack structures work; an unknown tag degrades to normal price with a logged warning — never to "unclaimable", per Phase 7's rule that a config problem must never brick a save.
  - **Detection stays off the hot path**, which is the design's biggest advantage over a neutral-territory model: `StructureManager` is queried only on a claim attempt (rare, gesture-driven, once per chunk), never on block break/place. No per-chunk cache, nothing for Phase 10's performance pass. Verified against the pinned `26.2` deobf jar: `startsForStructure(ChunkPos, Predicate<Structure>)` is the chunk-keyed entry point, with `hasAnyStructureAt`/`getStructureWithPieceAt`/`getAllStructuresAt` also available. The piece accessors on `StructureStart` still need the same verification pass before implementation.
  - **Rejected along the way: "neutral sites" that are unclaimable, unbreakable, and interact-only.** Its only delta from wild is unclaimability, so all of its cost is soft-lock risk and all of its benefit is flavor. Concretely it locks the End behind generation luck (a stronghold is a sealed shell — you could neither tunnel in nor place the eyes), makes mineshaft piece volumes permanently unmineable across huge invisible swathes of underground, and forbids repairing raid or creeper damage the world is still allowed to inflict.
- **Valor: a separate point/ranking system driving claim allowance and curve**, replacing (or supplementing) the Scepter-upgrade-only model — earned via scepter upgrades as today, plus other acts (small amount per XP gain, completing advancements, etc.), and usable as a PvP-world ranking metric independent of claims. A 2.0-scale idea: real to investigate only if the 1.0 upgrade-ladder curve doesn't feel satisfying in playtesting, since it adds a whole new tracked stat rather than reusing existing state.

## Multiplayer (data model is ready; everything else isn't)

- LAN/server support as a first-class mode: per-player empires already keyed by UUID (Phase 2).
- Teams/shared empires, ally permissions.
- Claim protection *from other players* — the classic direction, inverse of 1.0's model.

## UX & integration

- Management GUI replacing sneak-use gestures (claim map, unclaim buttons, upgrade screen).
- ~~ModMenu + Cloth Config settings screen~~ — **committed to 1.0 in [Phase 9](phase-9-commands-and-polish.md)** as an optional dependency, on the research's finding that a config screen is table stakes on Fabric.
- Map-mod integration: claim overlays for Xaero's/JourneyMap. **Slated as the first post-1.0 phase** — the research rates it table stakes, though calibrated to FTB Chunks where the map *is* the claim control surface; ours is positional, so 1.0 ships `/empiresmc claims` as the locator instead (see [Phase 9](phase-9-commands-and-polish.md)).
- Custom sound set replacing pitched vanilla sounds.
- Additional languages beyond `en_us` (community translations post-publish).
- Config format upgrade to TOML/JSON5 for comments (`configVersion` makes it mechanical). Lower priority since Phase 9 committed to a Cloth Config screen — tooltips cover the explanatory gap for anyone with the optional libraries, leaving this as a nicety for hand-editors.

## Technical

- Datapack-driven tier definitions for modpack authors (alongside, not replacing, config tiers). The research identifies modpack-friendliness as the growth lever — Chunk By Chunk's 44-modpack footprint came from being easy to embed — and rates this a fast-follow rather than a 1.0 blocker.
- Loom split source sets if client/server code separation gets hairy (fabric `DEV-008`).
- Fluid-flow and full indirect-edit tracking (register #6/#7) if players demand airtight wild protection.
- Minecraft version ports (policy set in Phase 1: port after 1.0, not during).
