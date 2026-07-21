package com.dakotatalley.empiresmc.claim

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import java.util.UUID

data class ClaimRecord(val owner: UUID, val claimedAtTick: Long) {
    companion object {
        val CODEC: Codec<ClaimRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(ClaimRecord::owner),
                Codec.LONG.fieldOf("claimed_at_tick").forGetter(ClaimRecord::claimedAtTick),
            ).apply(instance, ::ClaimRecord)
        }
    }
}
