package com.dakotatalley.empiresmc.claim

import java.util.UUID

// Transient, never-persisted per-player interaction state for the Scepter's use-gestures. Two
// concerns live here, both keyed by player: debouncing duplicate use() calls vanilla fires for a
// single physical click (both hands, held-button repeats), and the two-step sneak-use unclaim
// confirm. Pure Kotlin, no Minecraft types, so both are cheap to unit test - mirrors ClaimService's
// split between pure logic and its Minecraft glue.
class ClaimGestureService {
    private val lastGestureTick: MutableMap<UUID, Long> = mutableMapOf()
    private val pendingUnclaim: MutableMap<UUID, PendingUnclaim> = mutableMapOf()

    // One physical click can fire use() more than once for the same gesture (main hand then off
    // hand in the same tick, or a repeat a couple of ticks later) - collapse those into a single
    // action per player. Returns true (and records nothing) when the call should be ignored.
    fun shouldDebounce(player: UUID, tick: Long): Boolean {
        val last = lastGestureTick[player]
        if (last != null && tick - last < DEBOUNCE_TICKS) return true
        lastGestureTick[player] = tick
        return false
    }

    // Checks (and always clears) any pending unclaim confirm for this player. Returns true only
    // when it was for this exact chunk, hasn't expired, and MIN_CONFIRM_TICKS have passed since the
    // request - a mismatched chunk, a stale confirm, or one arriving too soon all fall through to
    // false, which the caller treats as "start a fresh prompt" rather than "commit." The floor
    // exists because vanilla auto-repeats a held use every ~4 ticks (QA/QC finding: held right-click
    // auto-repeats gestures) - without it, simply holding sneak-use down would satisfy both steps of
    // the confirm in well under a second. A rejected-for-too-soon attempt clears the old pending
    // entry, and the caller immediately re-requests on a false return, so a continuously held button
    // just keeps resetting its own clock and can never confirm; only a genuine release-and-reclick
    // after the floor can.
    fun confirmUnclaim(player: UUID, key: ClaimKey, tick: Long): Boolean {
        val pending = pendingUnclaim.remove(player) ?: return false
        val requestedAtTick = pending.expiresAtTick - CONFIRM_WINDOW_TICKS
        if (tick - requestedAtTick < MIN_CONFIRM_TICKS) return false
        return pending.key == key && tick <= pending.expiresAtTick
    }

    fun requestUnclaimConfirm(player: UUID, key: ClaimKey, tick: Long) {
        pendingUnclaim[player] = PendingUnclaim(key, tick + CONFIRM_WINDOW_TICKS)
    }

    // The backing item is a JVM-lifetime singleton (ModRegistry.SCEPTER), but a single-player
    // world's gameTime resets to (or starts fresh near) 0 on every new/reloaded world - without
    // this, a debounce or pending-confirm tick recorded against the previous world could still be
    // "in window" for the next one. Called on ServerLifecycleEvents.SERVER_STARTED.
    fun clear() {
        lastGestureTick.clear()
        pendingUnclaim.clear()
    }

    private data class PendingUnclaim(val key: ClaimKey, val expiresAtTick: Long)

    companion object {
        const val DEBOUNCE_TICKS = 5L
        const val CONFIRM_WINDOW_TICKS = 100L

        // Comfortably above vanilla's held-use repeat cadence (~4 ticks) so a held button can never
        // clear it, but far below CONFIRM_WINDOW_TICKS so a deliberate second click never feels
        // laggy.
        const val MIN_CONFIRM_TICKS = 20L
    }
}
