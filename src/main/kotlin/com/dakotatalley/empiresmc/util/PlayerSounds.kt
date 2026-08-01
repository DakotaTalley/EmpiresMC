/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.util

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

// Plays a personal UI-style sound cue to exactly one player, server-side.
//
// This exists because the obvious call - player.playSound(sound, volume, pitch) - silently does the
// opposite of what it reads like when invoked on the server. Player.playSound delegates to
// Level.playSound(entity = this, ...), and ServerLevel.playSeededSound turns that entity into the
// *exclusion* argument of PlayerList.broadcast (the loop skips the player it matches). So calling
// player.playSound(...) on the server plays the sound for every nearby player EXCEPT that player -
// in single-player, for nobody at all. Confirmed by decompiling Player.playSound,
// ServerLevel.playSeededSound and PlayerList.broadcast in the pinned 26.2 jar; it is the root cause
// of both the Phase 5 protection denials and the pre-existing Phase 4 Scepter feedback being silent
// during manual playtests.
//
// Vanilla's own answer to this used to be ServerPlayer.playNotifySound, which no longer exists in
// 26.2, so we send the packet it used to send. Addressing the player's connection directly also
// keeps the cue private, matching the action-bar messages it accompanies - a denial is feedback for
// the acting player, not an announcement to bystanders.
object PlayerSounds {
    fun playTo(player: ServerPlayer, sound: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
        player.connection.send(
            ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.PLAYERS,
                player.x,
                player.y,
                player.z,
                volume,
                pitch,
                player.random.nextLong(),
            ),
        )
    }
}
