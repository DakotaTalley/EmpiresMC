/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.claim

import com.dakotatalley.empiresmc.EmpiresMC
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

// The world-saved-data container for every dimension's claims and every player's empire profile
// (design decision: one global store, all dimensions - claim keys already carry the dimension).
// `service` is the only mutation surface callers should use: ClaimService.onMutate is wired to
// setDirty() here, so every successful claim/unclaim is automatically flagged for the next save -
// no call site outside this class needs to remember to mark anything dirty itself.
class ClaimData(
    val dataVersion: Int = CURRENT_DATA_VERSION,
    val claims: MutableMap<ClaimKey, ClaimRecord> = mutableMapOf(),
    val profiles: MutableMap<UUID, EmpireProfile> = mutableMapOf(),
) : SavedData() {

    val service: ClaimService = ClaimService(claims, profiles, onMutate = this::setDirty)

    companion object {
        // Our own schema version, independent of Minecraft's DataFixerUpper version - bump this
        // and branch on the decoded value if a future phase needs to migrate old claim data.
        const val CURRENT_DATA_VERSION = 1

        private data class ClaimEntry(val key: ClaimKey, val record: ClaimRecord)

        private val CLAIM_ENTRY_CODEC: Codec<ClaimEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                ClaimKey.CODEC.fieldOf("key").forGetter(ClaimEntry::key),
                ClaimRecord.CODEC.fieldOf("record").forGetter(ClaimEntry::record),
            ).apply(instance, ::ClaimEntry)
        }

        // Stored as a list of entries rather than Codec.unboundedMap: ClaimKey is a composite
        // (dimension, pos) key with no natural string form for a map key.
        private val CLAIMS_CODEC: Codec<MutableMap<ClaimKey, ClaimRecord>> = CLAIM_ENTRY_CODEC.listOf().xmap(
            { entries -> entries.associateTo(linkedMapOf()) { it.key to it.record } },
            { claims -> claims.map { (key, record) -> ClaimEntry(key, record) } },
        )

        private val PROFILES_CODEC: Codec<MutableMap<UUID, EmpireProfile>> =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, EmpireProfile.CODEC).xmap({ it.toMutableMap() }, { it })

        val CODEC: Codec<ClaimData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("data_version").forGetter(ClaimData::dataVersion),
                CLAIMS_CODEC.fieldOf("claims").forGetter(ClaimData::claims),
                PROFILES_CODEC.fieldOf("profiles").forGetter(ClaimData::profiles),
            ).apply(instance, ::ClaimData)
        }

        val TYPE: SavedDataType<ClaimData> = SavedDataType(
            Identifier.fromNamespaceAndPath(EmpiresMC.MOD_ID, "claims"),
            { ClaimData() },
            CODEC,
            DataFixTypes.LEVEL,
        )
    }
}
