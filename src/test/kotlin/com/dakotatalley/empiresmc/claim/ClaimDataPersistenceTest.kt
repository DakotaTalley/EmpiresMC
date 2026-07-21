package com.dakotatalley.empiresmc.claim

import com.dakotatalley.empiresmc.test.MinecraftBootstrapExtension
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier
import net.minecraft.util.datafix.DataFixers
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.storage.SavedDataStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

// Drives the real on-disk SavedData lifecycle that the pure-codec ClaimDataTest cannot: a genuine
// SavedDataStorage writes the .dat file on save, then a brand-new storage over the same folder
// reads it back. That fresh-storage read is exactly the quit/reload (and force-killed-after-save)
// path from the phase's exit criteria - if the file were never written, or written in a shape the
// codec can't parse, get() would return null here. RegistryAccess.EMPTY suffices because none of
// our codecs are registry-backed.
@ExtendWith(MinecraftBootstrapExtension::class)
class ClaimDataPersistenceTest {
    private val overworld = Identifier.withDefaultNamespace("overworld")

    private fun storage(dir: Path): SavedDataStorage =
        SavedDataStorage(dir, DataFixers.getDataFixer(), RegistryAccess.EMPTY)

    @Test
    fun claimStateWrittenOnSaveIsRecoveredByAFreshStorage(@TempDir dir: Path) {
        val owner = UUID.randomUUID()
        val key = ClaimKey(overworld, ChunkPos(7, -3))

        // First "process": attach the store, seed a claim + profile, then flush to disk.
        val writeStorage = storage(dir)
        val written = writeStorage.computeIfAbsent(ClaimData.TYPE)
        written.service.claim(owner, key, tick = 4242L)
        written.profiles[owner] = EmpireProfile(owner, scepterTier = 3, receivedScepter = true)
        written.setDirty()
        writeStorage.saveAndJoin()

        // Second "process": a fresh storage over the same folder must read the .dat back from disk.
        val reloaded = storage(dir).get(ClaimData.TYPE)

        assertNotNull(reloaded, "claim data .dat should have been written on save and re-read")
        assertEquals(ClaimRecord(owner, 4242L), reloaded!!.claims[key])
        assertEquals(EmpireProfile(owner, scepterTier = 3, receivedScepter = true), reloaded.profiles[owner])
        assertEquals(ClaimData.CURRENT_DATA_VERSION, reloaded.dataVersion)
        // The rebuilt service sees the reloaded map, not a fresh one.
        assertEquals(owner, reloaded.service.ownerOf(key))
    }
}
