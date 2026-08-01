/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
