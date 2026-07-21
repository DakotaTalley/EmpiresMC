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

    fun unclaim(player: UUID, key: ClaimKey): ClaimResult {
        val record = claims[key]
        if (record == null || record.owner != player) return ClaimResult.NotOwner
        claims.remove(key)
        onMutate()
        return ClaimResult.Success
    }

    companion object {
        const val STARTING_CLAIMS = 1

        // Placeholder grant per scepter tier - every tier currently grants the same amount until
        // Phase 8 replaces this with its config-driven tier table.
        const val CLAIMS_PER_TIER = 1
    }
}
