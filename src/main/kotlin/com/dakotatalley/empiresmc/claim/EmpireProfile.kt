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
