/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
