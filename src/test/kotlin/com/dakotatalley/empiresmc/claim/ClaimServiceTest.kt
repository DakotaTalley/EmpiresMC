package com.dakotatalley.empiresmc.claim

import net.minecraft.resources.Identifier
import net.minecraft.world.level.ChunkPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class ClaimServiceTest {
    private val overworld = Identifier.withDefaultNamespace("overworld")
    private val alice: UUID = UUID.randomUUID()
    private val bob: UUID = UUID.randomUUID()

    private fun key(x: Int, z: Int) = ClaimKey(overworld, ChunkPos(x, z))

    private fun service(
        claims: MutableMap<ClaimKey, ClaimRecord> = mutableMapOf(),
        profiles: MutableMap<UUID, EmpireProfile> = mutableMapOf(),
        onMutate: () -> Unit = {},
    ) = ClaimService(claims, profiles, onMutate)

    @Test
    fun claimGrantsOwnershipAndConsumesAllowance() {
        val claims = mutableMapOf<ClaimKey, ClaimRecord>()
        val service = service(claims = claims)
        val k = key(0, 0)

        val result = service.claim(alice, k, tick = 100L)

        assertEquals(ClaimResult.Success, result)
        assertEquals(alice, service.ownerOf(k))
        assertEquals(setOf(k), service.claimsOf(alice))
        assertEquals(0, service.remainingOf(alice))
        assertEquals(ClaimRecord(alice, 100L), claims[k])
    }

    @Test
    fun claimingAnAlreadyClaimedChunkIsDeniedRegardlessOfOwner() {
        val service = service()
        val k = key(1, 1)
        service.claim(alice, k, 0L)

        assertEquals(ClaimResult.AlreadyClaimed, service.claim(alice, k, 1L))
        assertEquals(ClaimResult.AlreadyClaimed, service.claim(bob, k, 1L))
    }

    @Test
    fun claimIsDeniedOnceAllowanceIsExhausted() {
        val service = service()
        service.claim(alice, key(0, 0), 0L)

        assertEquals(ClaimResult.NoAllowance, service.claim(alice, key(1, 0), 1L))
        assertEquals(0, service.remainingOf(alice))
    }

    @Test
    fun unclaimByTheOwnerReleasesTheChunk() {
        val service = service()
        val k = key(2, 2)
        service.claim(alice, k, 0L)

        val result = service.unclaim(alice, k)

        assertEquals(ClaimResult.Success, result)
        assertNull(service.ownerOf(k))
        assertEquals(emptySet<ClaimKey>(), service.claimsOf(alice))
        assertEquals(1, service.remainingOf(alice))
    }

    @Test
    fun unclaimByANonOwnerIsDenied() {
        val service = service()
        val k = key(3, 3)
        service.claim(alice, k, 0L)

        assertEquals(ClaimResult.NotOwner, service.unclaim(bob, k))
        assertEquals(alice, service.ownerOf(k))
    }

    @Test
    fun unclaimingAChunkNobodyOwnsIsDenied() {
        val service = service()
        assertEquals(ClaimResult.NotOwner, service.unclaim(alice, key(4, 4)))
    }

    @Test
    fun allowanceDerivesFromTier() {
        val profiles = mutableMapOf(alice to EmpireProfile(alice, scepterTier = 2, receivedScepter = true))
        val service = service(profiles = profiles)

        assertEquals(ClaimService.STARTING_CLAIMS + 2 * ClaimService.CLAIMS_PER_TIER, service.allowanceOf(alice))
    }

    @Test
    fun aPlayerWithNoProfileGetsTheStartingAllowance() {
        val service = service()
        assertEquals(ClaimService.STARTING_CLAIMS, service.allowanceOf(alice))
    }

    @Test
    fun claimsOfOnlyReturnsChunksOwnedByThatPlayer() {
        val service = service()
        service.claim(alice, key(0, 0), 0L)
        service.claim(bob, key(1, 1), 0L)

        assertEquals(setOf(key(0, 0)), service.claimsOf(alice))
        assertEquals(setOf(key(1, 1)), service.claimsOf(bob))
    }

    @Test
    fun remainingReflectsLiveClaimCountNotACachedCounter() {
        val profiles = mutableMapOf(alice to EmpireProfile(alice, scepterTier = 1, receivedScepter = true))
        val claims = mutableMapOf<ClaimKey, ClaimRecord>()
        val service = service(claims = claims, profiles = profiles)
        val k = key(0, 0)

        service.claim(alice, k, 0L)
        assertEquals(service.allowanceOf(alice) - 1, service.remainingOf(alice))

        // Mutate the backing map directly, bypassing the service, to prove remainingOf is
        // recomputed from the live map rather than reading a counter that could desync from it.
        claims.remove(k)
        assertEquals(service.allowanceOf(alice), service.remainingOf(alice))
    }

    @Test
    fun successfulMutationsInvokeTheDirtyMarkingHookDenialsDoNot() {
        var mutations = 0
        val service = service(onMutate = { mutations++ })
        val k = key(5, 5)

        service.claim(alice, k, 0L)
        assertEquals(1, mutations)

        service.claim(alice, k, 0L) // already claimed - denial, no mutation
        assertEquals(1, mutations)

        service.unclaim(bob, k) // not owner - denial, no mutation
        assertEquals(1, mutations)

        service.unclaim(alice, k)
        assertEquals(2, mutations)
    }
}
