/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
    fun unclaimByTheOwnerReleasesTheChunkOnceCooldownHasElapsed() {
        val service = service()
        val k = key(2, 2)
        service.claim(alice, k, 0L)

        val result = service.unclaim(alice, k, ClaimService.UNCLAIM_COOLDOWN_TICKS)

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

        assertEquals(ClaimResult.NotOwner, service.unclaim(bob, k, ClaimService.UNCLAIM_COOLDOWN_TICKS))
        assertEquals(alice, service.ownerOf(k))
    }

    @Test
    fun unclaimingAChunkNobodyOwnsIsDenied() {
        val service = service()
        assertEquals(ClaimResult.NotOwner, service.unclaim(alice, key(4, 4), 0L))
    }

    @Test
    fun unclaimIsDeniedBeforeTheCooldownElapsesAndReportsTheRemainingTime() {
        val service = service()
        val k = key(6, 6)
        service.claim(alice, k, 100L)

        val result = service.unclaim(alice, k, 100L + ClaimService.UNCLAIM_COOLDOWN_TICKS - 1)

        assertEquals(ClaimResult.OnCooldown(1L), result)
        assertEquals(alice, service.ownerOf(k), "a denied unclaim must not release the chunk")
    }

    @Test
    fun unclaimSucceedsExactlyOnTheCooldownBoundary() {
        val service = service()
        val k = key(7, 7)
        service.claim(alice, k, 100L)

        val result = service.unclaim(alice, k, 100L + ClaimService.UNCLAIM_COOLDOWN_TICKS)

        assertEquals(ClaimResult.Success, result)
    }

    @Test
    fun recordOfReturnsTheClaimRecordForAnOwnedChunkAndNullOtherwise() {
        val service = service()
        val k = key(8, 8)

        assertNull(service.recordOf(k))

        service.claim(alice, k, 42L)
        assertEquals(ClaimRecord(alice, 42L), service.recordOf(k))
    }

    @Test
    fun forceClaimBypassesAllowanceAndOverwritesAnExistingOwner() {
        var mutations = 0
        val service = service(onMutate = { mutations++ })
        val k = key(9, 9)
        service.claim(alice, k, 0L) // consumes alice's one starting allowance slot

        service.forceClaim(bob, k, 10L)

        assertEquals(ClaimRecord(bob, 10L), service.recordOf(k), "force-claim must overwrite the existing owner")
        assertEquals(2, mutations, "force-claim must still mark the container dirty")
    }

    @Test
    fun forceUnclaimBypassesTheCooldownAndReportsWhetherAnythingWasRemoved() {
        var mutations = 0
        val service = service(onMutate = { mutations++ })
        val k = key(10, 10)
        service.claim(alice, k, 0L) // cooldown has not elapsed

        assertEquals(true, service.forceUnclaim(k))
        assertNull(service.ownerOf(k))
        assertEquals(2, mutations)

        assertEquals(false, service.forceUnclaim(k), "nothing left to remove the second time")
        assertEquals(2, mutations, "a no-op force-unclaim must not mark dirty")
    }

    @Test
    fun formatCooldownMinutesFormatsWholeMinutesRoundedUp() {
        assertEquals("20", ClaimService.formatCooldownMinutes(ClaimService.TICKS_PER_DAY))
        assertEquals("10", ClaimService.formatCooldownMinutes(ClaimService.TICKS_PER_DAY / 2))
        assertEquals("1", ClaimService.formatCooldownMinutes(ClaimService.TICKS_PER_MINUTE))
        // A partial minute past a whole one still rounds up - never understates the wait.
        assertEquals("2", ClaimService.formatCooldownMinutes(ClaimService.TICKS_PER_MINUTE + 1))
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
    fun grantScepterIfNeededGrantsExactlyOnce() {
        var mutations = 0
        val service = service(onMutate = { mutations++ })

        assertEquals(true, service.grantScepterIfNeeded(alice))
        assertEquals(true, service.profileOf(alice).receivedScepter)
        assertEquals(1, mutations)

        assertEquals(false, service.grantScepterIfNeeded(alice))
        assertEquals(1, mutations, "a rejoin must not re-mark dirty or re-grant")
    }

    @Test
    fun grantScepterIfNeededPreservesExistingProfileFields() {
        val profiles = mutableMapOf(alice to EmpireProfile(alice, scepterTier = 2, receivedScepter = false))
        val service = service(profiles = profiles)

        assertEquals(true, service.grantScepterIfNeeded(alice))

        assertEquals(EmpireProfile(alice, scepterTier = 2, receivedScepter = true), service.profileOf(alice))
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

        service.unclaim(bob, k, ClaimService.UNCLAIM_COOLDOWN_TICKS) // not owner - denial, no mutation
        assertEquals(1, mutations)

        service.unclaim(alice, k, ClaimService.UNCLAIM_COOLDOWN_TICKS)
        assertEquals(2, mutations)
    }
}
