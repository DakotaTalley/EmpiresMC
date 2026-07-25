package com.dakotatalley.empiresmc.claim

import net.minecraft.resources.Identifier
import net.minecraft.world.level.ChunkPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ClaimGestureServiceTest {
    private val overworld = Identifier.withDefaultNamespace("overworld")
    private val alice: UUID = UUID.randomUUID()

    private fun key(x: Int, z: Int) = ClaimKey(overworld, ChunkPos(x, z))

    @Test
    fun firstGestureIsNeverDebounced() {
        val gestures = ClaimGestureService()
        assertFalse(gestures.shouldDebounce(alice, tick = 100L))
    }

    @Test
    fun aRepeatWithinTheDebounceWindowIsIgnored() {
        val gestures = ClaimGestureService()
        gestures.shouldDebounce(alice, tick = 100L)

        assertTrue(gestures.shouldDebounce(alice, tick = 100L + ClaimGestureService.DEBOUNCE_TICKS - 1))
    }

    @Test
    fun aGestureOnceTheDebounceWindowHasPassedIsNotDebounced() {
        val gestures = ClaimGestureService()
        gestures.shouldDebounce(alice, tick = 100L)

        assertFalse(gestures.shouldDebounce(alice, tick = 100L + ClaimGestureService.DEBOUNCE_TICKS))
    }

    @Test
    fun aFreshSneakUseHasNothingToConfirm() {
        val gestures = ClaimGestureService()
        assertFalse(gestures.confirmUnclaim(alice, key(0, 0), tick = 0L))
    }

    @Test
    fun repeatingTheSneakUseOnTheSameChunkWithinTheWindowConfirms() {
        val gestures = ClaimGestureService()
        val k = key(1, 1)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)

        assertTrue(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.CONFIRM_WINDOW_TICKS))
    }

    @Test
    fun aConfirmIsOneShotAndDoesNotCommitTwice() {
        val gestures = ClaimGestureService()
        val k = key(2, 2)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)
        assertTrue(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.MIN_CONFIRM_TICKS))

        assertFalse(
            gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.MIN_CONFIRM_TICKS + 1),
            "a second confirm check must not re-commit",
        )
    }

    @Test
    fun aConfirmArrivingBeforeTheMinimumWindowDoesNotCommit() {
        val gestures = ClaimGestureService()
        val k = key(8, 8)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)

        // Simulates a held button auto-repeating the sneak-use gesture: the second call arrives well
        // inside CONFIRM_WINDOW_TICKS but too soon to be a deliberate second click.
        assertFalse(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.MIN_CONFIRM_TICKS - 1))
    }

    @Test
    fun aHeldButtonCanNeverConfirmBecauseEachRejectedAttemptResetsItsOwnClock() {
        val gestures = ClaimGestureService()
        val k = key(9, 9)
        var tick = 100L
        gestures.requestUnclaimConfirm(alice, k, tick)

        // Mirrors ScepterItem's handleSneakUse: on a false confirm, it immediately re-requests. A
        // continuously held button (repeats every few ticks) should never clear MIN_CONFIRM_TICKS
        // before the next repeat resets the pending request.
        repeat(10) {
            tick += ClaimGestureService.MIN_CONFIRM_TICKS - 1
            assertFalse(gestures.confirmUnclaim(alice, k, tick))
            gestures.requestUnclaimConfirm(alice, k, tick)
        }
    }

    @Test
    fun sneakUsingADifferentChunkThanThePendingOneDoesNotConfirm() {
        val gestures = ClaimGestureService()
        gestures.requestUnclaimConfirm(alice, key(3, 3), tick = 100L)

        assertFalse(gestures.confirmUnclaim(alice, key(4, 4), tick = 100L + ClaimGestureService.MIN_CONFIRM_TICKS))
    }

    @Test
    fun aConfirmAfterTheWindowExpiresDoesNotCommit() {
        val gestures = ClaimGestureService()
        val k = key(5, 5)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)

        assertFalse(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.CONFIRM_WINDOW_TICKS + 1))
    }

    @Test
    fun anExpiredOrMismatchedConfirmStillClearsThePendingState() {
        val gestures = ClaimGestureService()
        val k = key(6, 6)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)
        gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.CONFIRM_WINDOW_TICKS + 1) // expired, clears it

        // A stale pending entry must not linger and later be confirmed by an unrelated call.
        assertFalse(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.CONFIRM_WINDOW_TICKS + 2))
    }

    @Test
    fun clearDropsDebounceStateSoATickThatWouldHaveBeenInWindowIsNotDebounced() {
        val gestures = ClaimGestureService()
        gestures.shouldDebounce(alice, tick = 100L)

        gestures.clear()

        assertFalse(gestures.shouldDebounce(alice, tick = 100L + ClaimGestureService.DEBOUNCE_TICKS - 1))
    }

    @Test
    fun clearDropsAPendingUnclaimConfirmSoItCanNoLongerBeCommitted() {
        val gestures = ClaimGestureService()
        val k = key(7, 7)
        gestures.requestUnclaimConfirm(alice, k, tick = 100L)

        gestures.clear()

        assertFalse(gestures.confirmUnclaim(alice, k, tick = 100L + ClaimGestureService.CONFIRM_WINDOW_TICKS))
    }
}
