# Phase 2 — Claim data & persistence

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 1](phase-1-project-scaffolding.md). Next: [Phase 3 — The Scepter](phase-3-the-scepter.md).

## Goal

The complete server-side claim model — who owns which chunks, how many chunks each player may claim — persisted safely in world saved data, with the logic unit-tested before any item or event code exists.

## Design decisions

- **Server-authoritative from day one.** Singleplayer still runs a logical server (fabric `SEC-001`), so all claim state lives and mutates server-side only. The client never holds authoritative data — Phase 6 only mirrors it for rendering.
- **Per-player state keyed by UUID, even though the mod targets single-player.** This costs nothing now and keeps LAN guests and a future multiplayer mode from requiring a data migration.
- **One global store, all dimensions.** A single `PersistentState` (or its Codec-based successor in the pinned version) attached to the overworld's state manager, holding every dimension's claims — claim keys carry the dimension id. Avoids per-world state managers and makes cross-dimension queries trivial.
- **Model:**
  - `ClaimKey(dimension: Identifier, pos: ChunkPos)`
  - `ClaimRecord(owner: UUID, claimedAtTick: Long)` — the tick timestamp drives the Phase 4 unclaim cooldown.
  - `EmpireProfile(player: UUID, scepterTier: Int, receivedScepter: Boolean)`
- **Derive, don't double-book.** Used-chunk count is derived by counting a player's claims; total allowance is derived from tier (base + per-tier grants, hardcoded constants until Phase 7/8). No stored counters that can desync from the claim map — a desynced counter is a free-chunks exploit.
- **`ClaimService` as pure Kotlin logic** (claim, unclaim, ownership query, allowance query) with the Minecraft glue (saved-data lifecycle, commands) in a thin layer around it — this is what makes JUnit coverage cheap.
- **Schema versioning from the first byte written:** a `dataVersion` int in the serialized root, so post-1.0 migrations are possible without heroics. Same discipline lands in the config file in Phase 7.

## Steps

- [ ] Implement `ClaimKey`, `ClaimRecord`, `EmpireProfile` as data classes (watch Kotlin `DEV-004`: everything semantically relevant goes in the primary constructor).
- [ ] Implement `ClaimService`: `claim(player, key)`, `unclaim(player, key)`, `ownerOf(key)`, `claimsOf(player)`, `allowanceOf(player)`, `remainingOf(player)` — returning typed results (success / not-owner / no-allowance / already-claimed) rather than booleans, so callers can produce specific feedback.
- [ ] Implement the saved-data container: NBT (de)serialization of the claim map + profiles + `dataVersion`, registered/attached on server start via the overworld state manager.
- [ ] Audit every mutation path to call the dirty-marking hook — a missed `markDirty` is silent data loss on unclean shutdown.
- [ ] Handle unknown dimension ids on load (dimension removed from the world/datapack): retain the records but log a warning; never crash or drop.
- [ ] Grant a starting allowance of 1 chunk (constant `STARTING_CLAIMS = 1`; moves to config in Phase 7). The starting chunk is *not* auto-claimed — the player places it deliberately in Phase 4.
- [ ] Add a permission-level-2 admin/debug command skeleton `/empiresmc admin` with `claiminfo` (chunk under player) and `profile <player>` — the dev-loop tool for every later phase.
- [ ] Unit tests (with the Phase 1 registry bootstrap helper): claim/unclaim accounting, allowance and remaining math, denial cases, serialization round-trip, unknown-dimension load, `dataVersion` present.

## Risks & flags

- **Missed dirty-marking** is the classic saved-data bug: everything works in a session, state vanishes after a crash. The audit step exists because this fails silently.
- **Storing derived counters** (used/remaining) instead of deriving them is the equivalent trap on the logic side — flagged here so it never creeps in during later phases.
- Chunk keys must include dimension from the start; retrofitting dimension-awareness after claims exist in saves would need a migration.
- Decision deferred to Phase 4 (flagged now): whether the first claim should auto-claim the spawn chunk. Recommendation is no — spawn terrain is random and the deliberate first placement is a meaningful choice.

## Exit criteria

- All `ClaimService` unit tests green in CI.
- In dev: claim state created via the admin command survives world save/quit/reload, and survives a force-killed process after a save.
