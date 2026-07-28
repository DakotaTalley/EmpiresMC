package com.dakotatalley.empiresmc.protection

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
        player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f)
    }

    private const val THROTTLE_TICKS = 20L
    private const val KEY_DENIED_WILD = "empiresmc.protection.denied_wild"
    private const val KEY_DENIED_OWNED = "empiresmc.protection.denied_owned"
}
