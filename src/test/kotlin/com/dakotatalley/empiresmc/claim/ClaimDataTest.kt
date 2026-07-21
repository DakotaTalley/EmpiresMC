package com.dakotatalley.empiresmc.claim

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.Identifier
import net.minecraft.world.level.ChunkPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ClaimDataTest {
    private val overworld = Identifier.withDefaultNamespace("overworld")

    @Test
    fun roundTripsClaimsProfilesAndDataVersionThroughNbt() {
        val data = ClaimData()
        val owner = UUID.randomUUID()
        data.claims[ClaimKey(overworld, ChunkPos(3, -2))] = ClaimRecord(owner, 1234L)
        data.profiles[owner] = EmpireProfile(owner, scepterTier = 1, receivedScepter = true)

        val encoded = ClaimData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()
        val decoded = ClaimData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow()

        assertEquals(data.dataVersion, decoded.dataVersion)
        assertEquals(data.claims, decoded.claims)
        assertEquals(data.profiles, decoded.profiles)
    }

    @Test
    fun encodedRootCarriesTheDataVersion() {
        val encoded = ClaimData.CODEC.encodeStart(NbtOps.INSTANCE, ClaimData()).getOrThrow() as CompoundTag

        assertTrue(encoded.contains("data_version"))
        assertEquals(ClaimData.CURRENT_DATA_VERSION, encoded.getInt("data_version").orElseThrow())
    }

    @Test
    fun claimReferencingAnUnknownDimensionRoundTripsWithoutError() {
        // ClaimKey stores a bare Identifier, not a ResourceKey<Level>, so the codec never validates
        // the dimension against a live registry: a claim for a dimension removed from the world or
        // datapack must still decode and be retained - never crash, never get silently dropped.
        val removedDimension = Identifier.fromNamespaceAndPath("someaddon", "removed_dimension")
        val data = ClaimData()
        val owner = UUID.randomUUID()
        val key = ClaimKey(removedDimension, ChunkPos(0, 0))
        data.claims[key] = ClaimRecord(owner, 0L)

        val encoded = ClaimData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()
        val decoded = ClaimData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow()

        assertEquals(ClaimRecord(owner, 0L), decoded.claims[key])
    }

    @Test
    fun successfulClaimsMarkTheContainerDirtyDenialsDoNot() {
        val data = ClaimData()
        assertFalse(data.isDirty)
        val owner = UUID.randomUUID()
        val key = ClaimKey(overworld, ChunkPos(0, 0))

        data.service.claim(owner, key, tick = 0L)
        assertTrue(data.isDirty)

        data.setDirty(false)
        data.service.claim(owner, key, tick = 1L) // already claimed - denial, should not dirty
        assertFalse(data.isDirty)

        data.service.unclaim(owner, key)
        assertTrue(data.isDirty)
    }
}
