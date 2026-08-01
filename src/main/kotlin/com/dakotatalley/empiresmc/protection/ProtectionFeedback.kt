/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.protection

import com.dakotatalley.empiresmc.util.PlayerSounds
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.Level
import java.util.UUID

// Throttled denial UX for every protection hook. Module-level object, not instance-scoped like
// ClaimGestureService - hooks here are static listeners (ProtectionHooks), not one item instance.
object ProtectionFeedback {
    private val lastDenialTick: MutableMap<UUID, Long> = mutableMapOf()

    // gameTime restarts near 0 for a new/reloaded single-player world - without this, a throttle
    // tick left over from the previous world could wrongly look "still in window" for the new one
    // (same reasoning as ClaimGestureService.clear()).
    fun initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register { lastDenialTick.clear() }
    }

    // ~1/sec throttle: PlayerBlockBreakEvents.BEFORE/AttackBlockCallback fire every tick while a
    // player holds left-click against a denied block - without this, denial spams the action bar.
    fun deny(player: ServerPlayer, level: Level, result: ProtectionResult) {
        if (result == ProtectionResult.Allowed) return
        val tick = level.gameTime
        val last = lastDenialTick[player.uuid]
        if (last != null && tick - last < THROTTLE_TICKS) return
        lastDenialTick[player.uuid] = tick
        val key = if (result == ProtectionResult.DeniedWild) KEY_DENIED_WILD else KEY_DENIED_OWNED
        player.sendSystemMessage(Component.translatable(key), true)
        PlayerSounds.playTo(player, SoundEvents.VILLAGER_NO)
    }

    // Corrects the client's optimistic prediction after a denied item use. The client runs the very
    // same Item.useOn/Item.use it just asked the server to run - MultiPlayerGameMode.performUseItemOn
    // and useItem call ItemStack.useOn/use on the *real* held stack, restoring the count only when
    // hasInfiniteMaterials() (creative) - so a denied placement shows the chest already gone from the
    // hotbar and a denied bucket shows an empty bucket. The mispredicted *block* is rolled back by the
    // prediction sequence ack, but the inventory is not: the server's stack never changed, so the menu
    // diffs clean against remoteSlots and never sends a correction. Push authoritative contents.
    //
    // Deliberately not throttled the way deny() is: the throttle exists only to stop action-bar/sound
    // spam, and skipping a resync would leave a real desync behind on every suppressed click. Only the
    // use hooks need this (once per click); break denial fires every tick and mispredicts nothing but
    // block state, which the sequence ack already handles.
    fun resyncInventory(player: ServerPlayer) {
        player.containerMenu.sendAllDataToRemote()
    }

    private const val THROTTLE_TICKS = 20L
    private const val KEY_DENIED_WILD = "empiresmc.protection.denied_wild"
    private const val KEY_DENIED_OWNED = "empiresmc.protection.denied_owned"
}
