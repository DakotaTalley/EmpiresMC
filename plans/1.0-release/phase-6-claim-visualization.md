# Phase 6 — Claim visualization

Part of the [EmpiresMC 1.0 release plan](../1.0-release.md). Previous: [Phase 5](phase-5-protection-enforcement.md). Next: [Phase 7 — Configuration](phase-7-configuration.md).

## Goal

Holding the Scepter shows the player's territory in-world: borders of owned claims, an outline of the chunk they're standing in, and live used/total numbers — backed by a real server→client sync layer.

## Design decisions

- **Sync layer first, rendering second.** Custom payloads (typed `CustomPayload` + the payload registry / `ServerPlayNetworking` in the pinned Fabric API): a full snapshot of the player's claims on join, dimension change, and respawn; small deltas on claim/unclaim/tier change. The client cache is display-only — the server re-validates everything (fabric `SEC-001`), so a tampered client can at most mis-render.
- **Render only while the Scepter is held** (either hand), per the project brief. Rendering via Fabric's world-render events in the client entrypoint package (fabric `DEV-002`: client-only code must not load server-side).
- **Border style: vertical wall-lines at claim edges, only on faces adjacent to unclaimed chunks** — interior borders between two owned chunks stay clean. Plus a distinct outline of the standing chunk when unclaimed: this is the claim preview that makes Phase 4's "claims target the standing chunk" legible before committing.
- **Don't rely on color alone** (constitution `A11Y-006` in spirit): owned-border and preview-outline differ in line style/animation, not just hue; colors configurable in Phase 7.
- **Render radius is capped** (default ~5 chunks around the player) so a late-game empire of dozens of chunks doesn't tank frame rate; cap configurable.
- HUD while holding: a small overlay with `used/total chunks · tier N`. The Phase 3 tooltip switches from static text to these live synced numbers.

## Steps

- [ ] Define payloads: `EmpireSnapshot` (claims for the player's current dimension + used/total + tier) and `EmpireDelta` (claim added/removed, tier changed); register codecs both sides.
- [ ] Server: send snapshot on join / dimension change / respawn; send deltas from the `ClaimService` mutation points.
- [ ] Client: cache keyed by dimension → chunk set + counters; cleared on disconnect.
- [ ] World rendering: border walls for owned claims (outward faces only), standing-chunk preview outline when unclaimed, within the render radius; verify depth behavior underground and at build height.
- [ ] HUD overlay + live tooltip from the client cache.
- [ ] Snapshot resync guard: client re-requests on cache-miss anomalies rather than trusting stale data.
- [ ] Tests: unit-test payload codecs round-trip; gametest that claim/unclaim emits the expected delta; manual render checklist (surface, underground, Nether, chunk borders at diagonal corners, radius cap).

## Risks & flags

- **Sync drift** (missed delta → phantom/missing borders) is a display bug but erodes trust in the claim system fast; the resync guard and mutation-point discipline (all sends from `ClaimService` hooks, never scattered) are the mitigations.
- Payload size: a snapshot is per-dimension and bounded by total claims (tens, not thousands) — fine for 1.0; flag only if backlog features (map integration) need more.
- Rendering cost scales with visible border faces; the radius cap plus outward-faces-only keeps it bounded. Profile in Phase 10 with 500+ claims.
- Line rendering through walls vs. depth-tested is a feel decision — prototype both, pick one, make the other a config option only if it's cheap.

## Exit criteria

- Holding the Scepter shows correct borders and preview in overworld and Nether; numbers match server truth after claim, unclaim, relog, and dimension hop.
- Not holding it renders nothing and costs nothing measurable.
- Payload/gametest coverage green in CI.
