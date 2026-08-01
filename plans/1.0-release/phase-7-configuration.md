# Phase 7 — Configuration

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 6](phase-6-claim-visualization.md). Next: [Phase 8 — Scepter upgrades](phase-8-scepter-upgrades.md).

## Goal

Every gameplay constant introduced in Phases 2–6 moves into a validated, reloadable config file — and the schema is ready to carry Phase 8's upgrade tier table, the single most balance-tweaked structure in the mod.

## Design decisions

- **Format: JSON at `config/empiresmc.json`** via `kotlinx.serialization` (already bundled by fabric-language-kotlin — no new dependency, satisfying constitution `SEC-005`). JSON's lack of comments is a known UX wart; mitigations: generated `_comment`-style hint fields plus a full config reference in the README (Phase 11), and — since Phase 9 now commits to a Cloth Config screen — per-option tooltips for anyone who has the optional libraries installed. TOML/JSON5 migration stays a backlog idea, cheap later because of `configVersion`, and less pressing now that the screen carries the explanatory burden for most users.
- **Config is untrusted input** (constitution `SEC-002`): every field validated and clamped with a logged warning; unknown fields tolerated (forward compat).
- **The file is canonical; [Phase 9](phase-9-commands-and-polish.md)'s Cloth Config screen is a front-end over it, never a second source of truth.** Two consequences to build for now, while the schema is being written: the screen must be *generated from* this schema so options can't drift between the two surfaces, and the screen's save path must run the same validation and clamping the loader does. Note the deliberate asymmetry with the next rule — a user pressing Save in a settings screen is explicit intent and legitimately rewrites the file; the prohibition below is on *silently* rewriting a file the user never asked us to touch.
- **A broken config file is never overwritten.** On parse failure: log loudly, keep the file untouched, run on last-known-good (or defaults). Auto-"fixing" by rewriting is how users lose an hour of hand-tuning to one typo.
- **`configVersion` int from day one**, mirroring Phase 2's `dataVersion`, so future schema migrations are mechanical.
- **Anti-churn is a per-claim item cost, not a refund percentage** (decided from the [market research](../research/market_research.md) review; supersedes the `unclaimRefundPercent` option this phase originally reserved). The research is right that full-refund-plus-cooldown is the weakest anti-abuse posture in the market, but a *percentage* refund is unrepresentable in this codebase: [Phase 2](phase-2-claim-data-and-persistence.md)'s "derive, don't double-book" rule means allowance is `startingClaims + tier grants` and used is `claimsOf().size`, so there is nowhere for "0.5 of a slot" to live short of adding the stored per-player counter Phase 2 deliberately refused. Charging an item per claim gets the same anti-nomadic effect with zero new state and no allowance math: unclaiming still refunds the slot in full, but *reclaiming* costs the item again, so churn has a real price. It is also visible to the player, which a fractional allowance penalty never would be. Implementation lands in [Phase 8](phase-8-scepter-upgrades.md) alongside the upgrade-cost machinery it shares; this phase only reserves the schema.
- **The claim cost is waived when the player owns zero chunks** (`waiveClaimCostWhenLandless`, default true). Without this, a player who has unclaimed their last chunk and cannot afford the cost item is hard soft-locked — no land to build or mine in, and no legal way to obtain the item, since [Phase 5](phase-5-protection-enforcement.md) forbids breaking blocks in the wild. Consequence worth stating up front: a tier-1 player (allowance 1) is *always* landless at the moment they claim, so their first chunk is permanently free and the cost only starts biting from tier 2 — a deliberate onramp, not a leak. The reclaim-for-free loop it opens is bounded by `unclaimCooldownTicks` and only ever available to a player who owns a single chunk; registered as an accepted row in the [Phase 10 exploit register](phase-10-exploit-hardening-and-balance.md).
  - **The waiver covers the base cost only.** Model any future surcharge — the structure-claim premium sketched in the [backlog](backlog.md) is the concrete case — as a component separate from the base cost, and waive only the base. Otherwise a landless player walks onto a stronghold or village chunk and takes the premium land for nothing. Denying the waiver there is safe precisely because it can never strand anyone: a landless player can always walk to ordinary ground and claim it free. Written down now, while `claimCostItems` is the only cost that exists, so the affordability code is shaped correctly from the first commit rather than retrofitted after the hole is found.
- **`wildDenyItems` exists because Phase 5's modifying-item deny-list is class-based** (flint & steel, hoe, shovel, axe, bone meal) and therefore blind to modded items that alter the world through `useOn`/`use`. A config id list lets modpack authors close gaps we can't enumerate; the residual gap gets documented in Phase 11's known-issues rather than pretended away (the research found modpack authors specifically look for that disclosure).
- **Reload semantics are explicit per option:** most options are read-on-use and hot-reload via `/empiresmc admin reload`; anything requiring restart (unlikely at this schema) is documented as such. Shrinking allowance below a player's current usage never revokes existing claims — it only blocks new ones.

### Option inventory (initial defaults in parentheses)

| Group | Options |
|---|---|
| Claims | `startingClaims` (1), `requireAdjacency` (false), `claimableDimensions` (all), `claimCostItems` (Phase 8 — schema reserved now), `waiveClaimCostWhenLandless` (true) |
| Unclaiming | `unclaimCooldownTicks` (24000) |
| Protection | `enforcementEnabled` (true), `explosionsBreakWild` (false), `wildBreakExceptions` (empty block list), `wildDenyItems` (empty item list), `creativeBypass` (true) |
| Rendering | `borderColor`, `previewColor`, `borderHeightBlocks` (8, the Y-clamp), `renderThroughWalls` (per Phase 6 decision) |
| HUD | `hudEnabled` (true), `hudPosition`, `hudShowWild` (true) |
| Upgrades | `tiers` (Phase 8 table — schema reserved now) |

## Steps

- [ ] Define the config data classes + serializer, defaults matching the constants currently in code.
- [ ] Loader: read at server/mod init, write a fully-populated default file if absent, apply validation/clamping with warnings, never overwrite an unparseable file.
- [ ] Replace every hardcoded constant from Phases 2–6 with config reads (grep for the Phase 2/4/5/6 constants; each phase doc named them).
- [ ] `/empiresmc admin reload` with a summary message of what changed; wire `wildBreakExceptions`, `wildDenyItems` and `explosionsBreakWild` into Phase 5's `ProtectionService` seam.
- [ ] Guard rails: allowance-shrink never revokes claims (test it); an unaffordable or unknown-item `claimCostItems` entry degrades to "claim is free" with a logged warning, never to "claiming is impossible" (a modpack removing the cost item must not brick a save).
- [ ] Unit tests: defaults round-trip, unknown-field tolerance, clamping, broken-file-preserved behavior, `configVersion` present, reload swaps values.

## Risks & flags

- **The config wipe failure mode** (rewriting the file on parse error) is the one users rage-quit over — it gets its own test.
- Hot-reload of protection toggles mid-session is safe because enforcement is stateless per event; hot-reload of `startingClaims` only affects *new* profiles — document this asymmetry in the file's hint fields.
- Item-id validation for `wildBreakExceptions` (and Phase 8 tier items) must happen after registries exist — validate at server start, warn and skip unknown ids, never crash (a modpack removing a mod shouldn't brick the save).
- Per-option reload semantics drift as options accumulate — the reload command's summary message is the living documentation; keep it honest.

## Exit criteria

- Deleting the config regenerates full defaults; a deliberately corrupted config boots the mod on defaults with the file untouched and a loud log line.
- Changing `unclaimCooldownTicks` + reload visibly changes Phase 4 behavior without a restart.
- No gameplay constant from Phases 2–6 remains hardcoded (verified by grep).
