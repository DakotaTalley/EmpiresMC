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

- [x] Implement `ClaimKey`, `ClaimRecord`, `EmpireProfile` as data classes (watch Kotlin `DEV-004`: everything semantically relevant goes in the primary constructor).
  - `claim/ClaimKey.kt` (`dimension: Identifier`, `pos: ChunkPos`), `claim/ClaimRecord.kt` (`owner: UUID`, `claimedAtTick: Long`), `claim/EmpireProfile.kt` (`player`, `scepterTier`, `receivedScepter`). All fields in the primary constructor.
  - Each carries a companion `CODEC` (added with the persistence step). None reference registry-backed types, so they (de)serialize with a bare `NbtOps` — no registry bootstrap needed to test them.
  - `claimedAtTick` is a `Long` to match `Level.getGameTime()` (the persisted, restart-surviving clock the Phase 4 cooldown compares against) — deliberately **not** `MinecraftServer.tickCount`, an `int` that resets to 0 each restart.
- [x] Implement `ClaimService`: `claim(player, key)`, `unclaim(player, key)`, `ownerOf(key)`, `claimsOf(player)`, `allowanceOf(player)`, `remainingOf(player)` — returning typed results (success / not-owner / no-allowance / already-claimed) rather than booleans, so callers can produce specific feedback.
  - `claim/ClaimService.kt` + `claim/ClaimResult.kt` (`sealed`: `Success`/`AlreadyClaimed`/`NotOwner`/`NoAllowance`). Pure Kotlin — takes the backing maps + an `onMutate: () -> Unit` hook as constructor params, with zero Minecraft persistence types, so it unit-tests without a server.
  - `claim(player, key, tick)` takes the tick as a parameter (stays MC-agnostic). Used/remaining are **derived** every call (`claimsOf().size`, tier math) — no stored counters that could desync.
  - Only four result variants as specced, so `unclaim` folds "nobody owns this chunk" into `NotOwner` rather than adding a fifth `NotClaimed`.
- [x] Implement the saved-data container: NBT (de)serialization of the claim map + profiles + `dataVersion`, registered/attached on server start via the overworld state manager.
  - MC `26.2` replaced the old `PersistentState`/`readNbt`/`writeNbt` pattern with a **Codec-based** `SavedData` + `SavedDataType` (the design decision's anticipated "Codec-based successor"). `claim/ClaimData.kt` is the `SavedData` subclass; its `CODEC` composes the per-type codecs and carries our own `data_version` (int) at the serialized root, independent of Minecraft's DataFixer version.
  - Claims serialize as a **list of `{key, record}` entries**, not `Codec.unboundedMap` — `ClaimKey` is a composite (dimension, pos) key with no natural string form. Profiles use `unboundedMap` keyed on the UUID string.
  - `claim/ClaimDataAccess.kt` is the only Minecraft glue: attaches via `server.overworld().dataStorage.computeIfAbsent(ClaimData.TYPE)` on `ServerLifecycleEvents.SERVER_STARTED`, wired from the `EmpiresMC` entrypoint. `SavedDataType` uses `DataFixTypes.LEVEL` (no modded option exists; harmless because same-version loads run no fixers).
- [x] Audit every mutation path to call the dirty-marking hook — a missed `markDirty` is silent data loss on unclean shutdown.
  - `ClaimService.onMutate` is wired to `SavedData::setDirty` in `ClaimData`, so every successful `claim`/`unclaim` marks the container dirty in one central place; denials do not (unit-tested both ways). Dirty is container-level, so any one mutation flushes the whole container.
  - **Flagged for Phase 3:** `ClaimData.claims`/`profiles` are exposed as public `MutableMap`s. Nothing mutates them directly today (the service is the sole writer), but Phase 3's first-join grant will edit `profiles` *without* a claim/unclaim — that path must call `setDirty()` itself (ideally via a dedicated profile-mutation method) or the grant is silently lost on an unclean shutdown.
- [x] Handle unknown dimension ids on load (dimension removed from the world/datapack): retain the records but log a warning; never crash or drop.
  - `ClaimKey` stores a bare `Identifier`, not a `ResourceKey<Level>`, so the codec never validates against a live registry — records for a removed dimension decode and are retained (unit-tested). `ClaimDataAccess` audits claimed dimensions against loaded levels on `SERVER_STARTED` and logs a warning per unknown dimension without dropping anything.
- [x] Grant a starting allowance of 1 chunk (constant `STARTING_CLAIMS = 1`; moves to config in Phase 7). The starting chunk is *not* auto-claimed — the player places it deliberately in Phase 4.
  - `ClaimService.STARTING_CLAIMS = 1`; `allowanceOf = STARTING_CLAIMS + scepterTier * CLAIMS_PER_TIER`. `CLAIMS_PER_TIER = 1` is an inert placeholder (nothing sets `scepterTier > 0` until Phase 8's config-driven tier table). No auto-claim.
- [x] Add a permission-level-2 admin/debug command skeleton `/empiresmc admin` with `claiminfo` (chunk under player) and `profile <player>` — the dev-loop tool for every later phase.
  - `command/AdminCommand.kt`, registered via `CommandRegistrationCallback`, gated at `Commands.LEVEL_GAMEMASTERS` (MC `26.2`'s named `PermissionLevel.GAMEMASTERS` == the old op level 2). Both subcommands are **read-only** (see the exit-criteria note).
- [x] Unit tests (with the Phase 1 registry bootstrap helper): claim/unclaim accounting, allowance and remaining math, denial cases, serialization round-trip, unknown-dimension load, `dataVersion` present.
  - `ClaimServiceTest` (11): accounting, allowance/remaining math, every denial case, derived-not-cached remaining, dirty-hook-on-success-only. No bootstrap needed (pure logic).
  - `ClaimDataTest` (4): codec round-trip, `data_version` at the root, unknown-dimension round-trip, dirty-on-mutation.
  - `ClaimDataPersistenceTest` (1): real on-disk lifecycle — a genuine `SavedDataStorage` writes the `.dat` on save and a **fresh** storage reads it back (the quit/reload + force-killed-after-save path). Added during the final sweep to cover a gap the codec-only test missed.
  - `EmpiresMCGameTest.adminCommandsExecuteSuccessfully`: drives both admin subcommands through the real dispatcher against a live server (beyond the checklist, but cheap regression cover for command wiring).

## Risks & flags

- **Missed dirty-marking** is the classic saved-data bug: everything works in a session, state vanishes after a crash. The audit step exists because this fails silently.
- **Storing derived counters** (used/remaining) instead of deriving them is the equivalent trap on the logic side — flagged here so it never creeps in during later phases.
- Chunk keys must include dimension from the start; retrofitting dimension-awareness after claims exist in saves would need a migration.
- Decision deferred to Phase 4 (flagged now): whether the first claim should auto-claim the spawn chunk. Recommendation is no — spawn terrain is random and the deliberate first placement is a meaningful choice.

## Exit criteria

- [x] All `ClaimService` unit tests green in CI.
  - Locally green: `./gradlew clean build` passes with 18 unit tests (0 failures) across `ClaimServiceTest`/`ClaimDataTest`/`ClaimDataPersistenceTest`/`EmpiresMCTest`, plus 3 gametests. CI runs the same `build` on push/PR (Phase 1 workflow) — confirm the run there after pushing the branch.
- [x] In dev: claim state created via the admin command survives world save/quit/reload, and survives a force-killed process after a save.
  - **Verified as a property, not via the literal admin-command flow.** `ClaimDataPersistenceTest` exercises the exact persistence guarantee end-to-end: a real `SavedDataStorage` writes the `.dat` on save, and a brand-new storage over the same folder reads the seeded claim + profile back — which is precisely what quit/reload and a force-killed-after-save process do. The `SERVER_STARTED` → `computeIfAbsent` attach path is separately exercised by the gametest server.
  - **Flagged inconsistency:** the criterion says "claim state created *via the admin command*," but the admin command as specced (`claiminfo` + `profile`) is **read-only** — there is no runtime way to create a claim until Phase 4 adds claiming. The manual "create in dev, restart, observe" flow is therefore not possible in Phase 2.
  - **Resolution:** Phase 2 stands on the integration test as equivalent verification; the debug `/empiresmc admin claim|unclaim` command is scheduled as a step in [Phase 4](phase-4-claiming-and-unclaiming.md), which makes the literal manual create → save → reload → observe flow reproducible from that phase on.
