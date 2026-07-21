package com.dakotatalley.empiresmc.claim

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import java.util.UUID

data class EmpireProfile(val player: UUID, val scepterTier: Int, val receivedScepter: Boolean) {
    companion object {
        val CODEC: Codec<EmpireProfile> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(EmpireProfile::player),
                Codec.INT.fieldOf("scepter_tier").forGetter(EmpireProfile::scepterTier),
                Codec.BOOL.fieldOf("received_scepter").forGetter(EmpireProfile::receivedScepter),
            ).apply(instance, ::EmpireProfile)
        }
    }
}
