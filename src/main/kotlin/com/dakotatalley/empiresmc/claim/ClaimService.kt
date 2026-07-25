package com.dakotatalley.empiresmc.claim

import java.util.UUID

// Pure Kotlin claim/allowance logic, deliberately free of Minecraft's SavedData/NBT/Codec types
// so it's cheap to unit test. Callers own the backing maps and the dirty-marking hook that fires
// on every successful mutation - the persistence layer wires that hook to SavedData.setDirty().
class ClaimService(
    private val claims: MutableMap<ClaimKey, ClaimRecord>,
    private val profiles: MutableMap<UUID, EmpireProfile>,
    private val onMutate: () -> Unit = {},
) {
    fun ownerOf(key: ClaimKey): UUID? = claims[key]?.owner

    fun recordOf(key: ClaimKey): ClaimRecord? = claims[key]

    fun claimsOf(player: UUID): Set<ClaimKey> = claims.filterValues { it.owner == player }.keys

    fun profileOf(player: UUID): EmpireProfile =
        profiles[player] ?: EmpireProfile(player, scepterTier = 0, receivedScepter = false)

    // Derived from tier every call, never cached - a stored counter could desync from the claim
    // map and hand out free chunks.
    fun allowanceOf(player: UUID): Int =
        STARTING_CLAIMS + profileOf(player).scepterTier * CLAIMS_PER_TIER

    fun remainingOf(player: UUID): Int = allowanceOf(player) - claimsOf(player).size

    // The sole write path for the first-join Scepter grant: flips the flag exactly once so a
    // rejoin never re-grants (recovery goes through the crafting recipe instead). Returns whether
    // this call actually granted it, so the caller knows whether to give the item.
    fun grantScepterIfNeeded(player: UUID): Boolean {
        val profile = profileOf(player)
        if (profile.receivedScepter) return false
        profiles[player] = profile.copy(receivedScepter = true)
        onMutate()
        return true
    }

    fun claim(player: UUID, key: ClaimKey, tick: Long): ClaimResult {
        if (claims.containsKey(key)) return ClaimResult.AlreadyClaimed
        if (remainingOf(player) <= 0) return ClaimResult.NoAllowance
        claims[key] = ClaimRecord(player, tick)
        onMutate()
        return ClaimResult.Success
    }

    // The unclaim cooldown runs on world game time (tick), not wall-clock - see the Phase 4 design
    // decision. Gated here, not just at the gesture layer, so the rule holds for every caller.
    fun unclaim(player: UUID, key: ClaimKey, tick: Long): ClaimResult {
        val record = claims[key]
        if (record == null || record.owner != player) return ClaimResult.NotOwner
        val remaining = remainingCooldownTicks(record, tick)
        if (remaining > 0) return ClaimResult.OnCooldown(remaining)
        claims.remove(key)
        onMutate()
        return ClaimResult.Success
    }

    // Raw seeding/override for the admin debug command (Phase 4): bypasses allowance and, for
    // unclaim, the cooldown gate entirely. Still routes through the shared onMutate hook so a
    // force-mutation is dirty-marked and persisted exactly like a gameplay one - skipping the
    // gates must never mean skipping the save.
    fun forceClaim(player: UUID, key: ClaimKey, tick: Long) {
        claims[key] = ClaimRecord(player, tick)
        onMutate()
    }

    fun forceUnclaim(key: ClaimKey): Boolean {
        val removed = claims.remove(key) != null
        if (removed) onMutate()
        return removed
    }

    companion object {
        // Shared by the unclaim gate and the status readout so they can't drift apart on the
        // cooldown math (QA/QC finding: duplicated cooldown-remaining math).
        fun remainingCooldownTicks(record: ClaimRecord, tick: Long): Long =
            UNCLAIM_COOLDOWN_TICKS - (tick - record.claimedAtTick)


        const val STARTING_CLAIMS = 1

        // Placeholder grant per scepter tier - every tier currently grants the same amount until
        // Phase 8 replaces this with its config-driven tier table.
        const val CLAIMS_PER_TIER = 1

        const val TICKS_PER_DAY = 24_000L

        // Real-time tick rate (20 ticks/sec), used to render the cooldown in minutes rather than
        // in-game days - sleep and /time set don't move getGameTime(), so a "days" label would
        // mislead players into thinking the cooldown tracks the daylight cycle.
        const val TICKS_PER_MINUTE = 1_200L

        // Dev default: 1 in-game day per chunk from the moment it was claimed; configurable in
        // Phase 7.
        const val UNCLAIM_COOLDOWN_TICKS = TICKS_PER_DAY

        // Shared by the unclaim-denial and status-gesture feedback so both read the same rounding.
        // Whole minutes, rounded up: never reports fewer minutes than are actually left. Callers
        // only invoke this for remaining >= TICKS_PER_MINUTE - anything under a minute should use
        // a "less than a minute" message instead, since rounding a sub-minute remainder up to "1"
        // would overstate how long is left.
        fun formatCooldownMinutes(ticks: Long): String =
            Math.ceil(ticks.toDouble() / TICKS_PER_MINUTE).toLong().toString()
    }
}
