# Phase 7 — Configuration

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 6](phase-6-claim-visualization.md). Next: [Phase 8 — Scepter upgrades](phase-8-scepter-upgrades.md).

## Goal

Every gameplay constant introduced in Phases 2–6 moves into a validated, reloadable config file — and the schema is ready to carry Phase 8's upgrade tier table, the single most balance-tweaked structure in the mod.

## Design decisions

- **Format: JSON at `config/empiresmc.json`** via `kotlinx.serialization` (already bundled by fabric-language-kotlin — no new dependency, satisfying constitution `SEC-005`). JSON's lack of comments is a known UX wart; mitigations: generated `_comment`-style hint fields plus a full config reference in the README (Phase 11). TOML/JSON5 migration is a backlog idea, cheap later because of `configVersion`.
- **Config is untrusted input** (constitution `SEC-002`): every field validated and clamped with a logged warning; unknown fields tolerated (forward compat).
- **A broken config file is never overwritten.** On parse failure: log loudly, keep the file untouched, run on last-known-good (or defaults). Auto-"fixing" by rewriting is how users lose an hour of hand-tuning to one typo.
- **`configVersion` int from day one**, mirroring Phase 2's `dataVersion`, so future schema migrations are mechanical.
- **Reload semantics are explicit per option:** most options are read-on-use and hot-reload via `/empiresmc admin reload`; anything requiring restart (unlikely at this schema) is documented as such. Shrinking allowance below a player's current usage never revokes existing claims — it only blocks new ones.

### Option inventory (initial defaults in parentheses)

| Group | Options |
|---|---|
| Claims | `startingClaims` (1), `requireAdjacency` (false), `claimableDimensions` (all) |
| Unclaiming | `unclaimCooldownTicks` (24000), `unclaimRefundPercent` (100) |
| Protection | `enforcementEnabled` (true), `explosionsBreakWild` (false), `wildBreakExceptions` (empty block list), `creativeBypass` (true) |
| Rendering | `borderColor`, `previewColor`, `renderRadiusChunks` (5), `renderThroughWalls` (per Phase 6 decision) |
| Upgrades | `tiers` (Phase 8 table — schema reserved now) |

## Steps

- [ ] Define the config data classes + serializer, defaults matching the constants currently in code.
- [ ] Loader: read at server/mod init, write a fully-populated default file if absent, apply validation/clamping with warnings, never overwrite an unparseable file.
- [ ] Replace every hardcoded constant from Phases 2–6 with config reads (grep for the Phase 2/4/5/6 constants; each phase doc named them).
- [ ] `/empiresmc admin reload` with a summary message of what changed; wire `wildBreakExceptions` and `explosionsBreakWild` into Phase 5's `ProtectionService` seam.
- [ ] Guard rails: allowance-shrink never revokes claims (test it); refund-percent rounding always favors the player having whole chunks.
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
