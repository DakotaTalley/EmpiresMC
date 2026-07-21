package com.dakotatalley.empiresmc.claim

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.Identifier
import net.minecraft.world.level.ChunkPos

data class ClaimKey(val dimension: Identifier, val pos: ChunkPos) {
    companion object {
        val CODEC: Codec<ClaimKey> = RecordCodecBuilder.create { instance ->
            instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(ClaimKey::dimension),
                ChunkPos.CODEC.fieldOf("pos").forGetter(ClaimKey::pos),
            ).apply(instance, ::ClaimKey)
        }
    }
}
