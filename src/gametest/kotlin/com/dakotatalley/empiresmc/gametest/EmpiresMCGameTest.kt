package com.dakotatalley.empiresmc.gametest

import com.dakotatalley.empiresmc.EmpiresMC
import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.claim.ClaimGestureService
import com.dakotatalley.empiresmc.claim.ClaimKey
import com.dakotatalley.empiresmc.claim.ClaimResult
import com.dakotatalley.empiresmc.claim.ClaimService
import com.dakotatalley.empiresmc.registry.ModRegistry
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.LevelBasedPermissionSet
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.gamerules.GameRules
import kotlin.random.Random

class EmpiresMCGameTest {
    @GameTest
    fun modIsLoaded(helper: GameTestHelper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded(EmpiresMC.MOD_ID), "EmpiresMC should be loaded")
        helper.succeed()
    }

    private fun countScepters(player: ServerPlayer): Int {
        var count = 0
        for (slot in 0 until player.inventory.containerSize) {
            if (player.inventory.getItem(slot).item === ModRegistry.SCEPTER) count++
        }
        return count
    }

    // Invoking ServerPlayerEvents.JOIN directly (rather than relying on whatever join path
    // makeMockServerPlayerInLevel exercises) drives exactly the same listener production code
    // registers in ScepterGrant, and stays correct either way: if the mock player's placement
    // already fired JOIN once, this is an idempotent no-op re-invocation per grantScepterIfNeeded's
    // flag (ClaimServiceTest.grantScepterIfNeededGrantsExactlyOnce covers that in isolation).
    @GameTest
    fun freshPlayerReceivesExactlyOneScepterOnJoin(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()

        ServerPlayerEvents.JOIN.invoker().onJoin(player)

        helper.assertTrue(countScepters(player) == 1, "expected exactly 1 Scepter after first join, found ${countScepters(player)}")
        helper.succeed()
    }

    @GameTest
    fun rejoinGrantsNoAdditionalScepter(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()

        ServerPlayerEvents.JOIN.invoker().onJoin(player)
        ServerPlayerEvents.JOIN.invoker().onJoin(player)

        helper.assertTrue(countScepters(player) == 1, "rejoin must not grant an additional Scepter, found ${countScepters(player)}")
        helper.succeed()
    }

    // Keep-on-death is now a re-grant on respawn (no mixin): ScepterGrant listens on AFTER_RESPAWN
    // and hands an entitled player a fresh Scepter when they come back without one - the
    // keepInventory-off death case, where vanilla's ServerPlayer.restoreFrom does not copy the old
    // inventory across. These drive the listener directly, the same way the JOIN tests above do;
    // Fabric's PlayerListMixin fires AFTER_RESPAWN at the tail of PlayerList.respawn in production,
    // which is the seam we're trusting (verified against the Fabric API jar for the pinned version).
    //
    // Each test invokes JOIN explicitly first so the "entitled" precondition (receivedScepter flag)
    // holds regardless of whether makeMockServerPlayerInLevel's placement already fired JOIN - the
    // grant is idempotent, so the count is 1 either way.
    @GameTest
    fun respawnRestoresScepterWhenPlayerCameBackWithoutOne(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        ServerPlayerEvents.JOIN.invoker().onJoin(player)
        // A keepInventory-off death drops the whole inventory; model the respawned player's empty
        // starting state so only the restore listener can put a Scepter back.
        player.inventory.clearContent()

        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(player, player, false)

        helper.assertTrue(
            countScepters(player) == 1,
            "respawn should restore exactly 1 Scepter to an entitled player, found ${countScepters(player)}",
        )
        helper.succeed()
    }

    @GameTest
    fun respawnDoesNotDuplicateScepterThatSurvived(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        ServerPlayerEvents.JOIN.invoker().onJoin(player)

        // keepInventory-on death and returning from the End both leave the Scepter in hand (vanilla
        // copies the inventory), so the "already holding one" guard must make this a no-op, not a
        // second handout.
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(player, player, false)

        helper.assertTrue(
            countScepters(player) == 1,
            "respawn must not duplicate a surviving Scepter, found ${countScepters(player)}",
        )
        helper.succeed()
    }

    // End-to-end cover for the AFTER_RESPAWN wiring: unlike the invoker-driven tests above, this
    // drives a genuine keepInventory-off death followed by a real PlayerList.respawn, so the restore
    // has to survive the full machinery - a brand-new ServerPlayer instance, restoreFrom declining
    // to copy the old inventory across, and Fabric actually firing AFTER_RESPAWN at respawn's tail.
    // Kills with die() rather than hurtServer() because a mock player is permanently invulnerable to
    // hurtServer() (fabric DEV-015).
    @GameTest
    fun realDeathRespawnRestoresExactlyOneScepter(helper: GameTestHelper) {
        val original = helper.makeMockServerPlayerInLevel()
        ServerPlayerEvents.JOIN.invoker().onJoin(original)
        helper.level.gameRules.set(GameRules.KEEP_INVENTORY, false, helper.level.server)

        original.die(helper.level.damageSources().genericKill())
        val respawned = helper.level.server.playerList.respawn(original, false, Entity.RemovalReason.KILLED)

        helper.assertTrue(
            countScepters(respawned) == 1,
            "a real keepInventory-off death and respawn should leave exactly 1 Scepter, found ${countScepters(respawned)}",
        )
        helper.succeed()
    }

    // Recovery of last resort: confirms the data-driven recipe actually loaded (a malformed
    // ingredient/result shape fails silently in the datapack loader otherwise) rather than
    // asserting on crafting-grid interaction, which needs no GameTest-level API here.
    @GameTest
    fun recoveryRecipeIsLoaded(helper: GameTestHelper) {
        val recipeId = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EmpiresMC.MOD_ID, "scepter"))
        val recipe = helper.level.server.recipeManager.byKey(recipeId)
        helper.assertTrue(recipe.isPresent, "expected the empiresmc:scepter recovery recipe to be loaded")
        helper.succeed()
    }

    // Drives the raw dispatcher directly (bypassing Commands.performCommand's broad catch, which
    // hides real exceptions behind a client-only hover tooltip) so a genuine bug in either command
    // body surfaces here as a real, visible test failure.
    @GameTest
    fun adminCommandsExecuteSuccessfully(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        val source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER)
        val dispatcher = helper.level.server.commands.dispatcher
        dispatcher.execute("empiresmc admin claiminfo", source)
        dispatcher.execute("empiresmc admin profile ${player.scoreboardName}", source)
        dispatcher.execute("empiresmc admin scepter", source)
        helper.succeed()
    }

    // GameTest runs many test methods concurrently against one shared MinecraftServer and world, so
    // every mock player - regardless of which test structure it nominally belongs to - is placed at
    // literal world position (0, 0, 0), not somewhere relative to its own structure (confirmed from
    // the test server's join log). ClaimDataAccess.of(server) is likewise one shared ClaimService
    // for the whole batch, and its state is only as fresh as the local gametest world on disk - a
    // repeated local `./gradlew build` without a clean reuses whatever a prior run already
    // persisted there. A random chunk far from the origin (and from any other random chunk this
    // suite hands out) keeps every test's claims from colliding with any of that, in-run or across
    // runs, without needing to coordinate literal indices by hand.
    private fun randomChunkKey(helper: GameTestHelper): ClaimKey =
        ClaimKey(helper.level.dimension().identifier(), ChunkPos(Random.nextInt(100_000, 10_000_000), 0))

    private fun ownChunkKey(helper: GameTestHelper, player: ServerPlayer): ClaimKey {
        val key = randomChunkKey(helper)
        player.setPos((key.pos.x * 16 + 8).toDouble(), 64.0, 8.0)
        return key
    }

    // Phase 4: driving ScepterItem.use() directly is the same technique the Phase 3 tests above use
    // for Fabric events - it exercises the real production entry point without needing a simulated
    // client packet round trip.
    @GameTest
    fun useOnAnUnclaimedChunkClaimsIt(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND)

        helper.assertTrue(service.ownerOf(key) == player.uuid, "expected the standing chunk to be claimed by the player")
        helper.succeed()
    }

    @GameTest
    fun useIsDeniedOnceTheStartingAllowanceIsExhausted(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        val service = ClaimDataAccess.of(helper.level.server).service
        val ownKey = ownChunkKey(helper, player)
        // Exhaust the starting allowance (1 chunk) with a claim elsewhere before exercising the
        // gesture on the standing chunk.
        service.forceClaim(player.uuid, randomChunkKey(helper), 0L)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND)

        helper.assertTrue(service.ownerOf(ownKey) == null, "no allowance remains, the standing chunk must stay unclaimed")
        helper.succeed()
    }

    @GameTest
    fun useOnAnOwnClaimedChunkShowsStatusWithoutMutatingIt(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        service.forceClaim(player.uuid, key, helper.level.gameTime)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND)

        helper.assertTrue(service.ownerOf(key) == player.uuid, "a status-only use() on an owned chunk must not change ownership")
        helper.succeed()
    }

    // QA/QC finding: the only reachable, player-facing Phase 4 gesture left without an end-to-end
    // test - sneak-use on a chunk nobody owns yet must fall into the upgrade_reserved_unclaimed
    // branch of handleSneakUse, not the claim path plain use() exercises above, and must leave the
    // chunk unclaimed either way.
    @GameTest
    fun sneakUseOnAnUnclaimedChunkDoesNotClaimIt(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        player.setShiftKeyDown(true)
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND)

        helper.assertTrue(service.ownerOf(key) == null, "sneak-use on an unclaimed chunk must not claim it")
        helper.succeed()
    }

    @GameTest
    fun sneakUseTwiceIsDeniedBeforeTheCooldownElapses(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        player.setShiftKeyDown(true)
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        service.forceClaim(player.uuid, key, helper.level.gameTime) // just claimed - cooldown has not elapsed

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // first sneak-use: prompt
        helper.runAfterDelay(ClaimGestureService.DEBOUNCE_TICKS + 1) {
            ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // second: commit attempt

            helper.assertTrue(service.ownerOf(key) == player.uuid, "a cooldown denial must not unclaim the chunk")
            helper.succeed()
        }
    }

    // GameTest exposes no public API to fast-forward Level.getGameTime() itself (GameTestHelper's
    // setTime() only advances the separate day/night clock), so this seeds the claim's own
    // timestamp in the past via the same force-claim path the admin debug command uses, rather than
    // waiting out a real in-game day of ticks.
    @GameTest(maxTicks = 40)
    fun sneakUseTwiceUnclaimsAfterTheCooldownHasElapsedAndTheSlotIsClaimableElsewhere(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        player.setShiftKeyDown(true)
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        service.forceClaim(player.uuid, key, helper.level.gameTime - ClaimService.UNCLAIM_COOLDOWN_TICKS)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // first sneak-use: prompt
        // MIN_CONFIRM_TICKS+1, not DEBOUNCE_TICKS+1: a deliberate second click must clear the floor
        // that keeps a held button from auto-repeating its way through the confirm (QA/QC finding).
        helper.runAfterDelay(ClaimGestureService.MIN_CONFIRM_TICKS + 1) {
            ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // second: commits

            helper.assertTrue(service.ownerOf(key) == null, "expected the chunk to be unclaimed")
            val elsewhere = randomChunkKey(helper)
            helper.assertTrue(
                service.claim(player.uuid, elsewhere, helper.level.gameTime) == ClaimResult.Success,
                "the refunded allowance slot should be claimable elsewhere",
            )
            helper.succeed()
        }
    }

    // Simulates a held button: vanilla auto-repeats a held use every ~4 ticks, which clears the
    // 5-tick debounce but is far under MIN_CONFIRM_TICKS (QA/QC finding: held right-click
    // auto-repeats gestures). Cooldown is pre-elapsed, so a commit here would only be blocked by the
    // confirm floor - isolates the behaviour under test from the unclaim cooldown.
    @GameTest(maxTicks = 150)
    fun heldSneakUseRepeatsDoNotConfirmUntilTheMinimumConfirmWindowPasses(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        player.setShiftKeyDown(true)
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        service.forceClaim(player.uuid, key, helper.level.gameTime - ClaimService.UNCLAIM_COOLDOWN_TICKS)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // first sneak-use: prompt
        helper.runAfterDelay(ClaimGestureService.DEBOUNCE_TICKS + 1) {
            ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // repeat: too soon to confirm

            helper.assertTrue(service.ownerOf(key) == player.uuid, "a repeat arriving before MIN_CONFIRM_TICKS must not unclaim")
            helper.runAfterDelay(ClaimGestureService.MIN_CONFIRM_TICKS + 1) {
                ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // deliberate second click: commits

                helper.assertTrue(service.ownerOf(key) == null, "a click past the floor should still be able to commit")
                helper.succeed()
            }
        }
    }

    // Cooldown is pre-elapsed via the same force-claim seeding as the test above, so the only
    // reason this second sneak-use can fail to commit is the expired confirm window, not the
    // unclaim cooldown - isolates the behaviour under test.
    @GameTest(maxTicks = 150)
    fun sneakUseAfterTheConfirmWindowExpiresRePromptsInsteadOfUnclaiming(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        player.setShiftKeyDown(true)
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        service.forceClaim(player.uuid, key, helper.level.gameTime - ClaimService.UNCLAIM_COOLDOWN_TICKS)

        ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // first sneak-use: prompt
        helper.runAfterDelay(ClaimGestureService.CONFIRM_WINDOW_TICKS + ClaimGestureService.DEBOUNCE_TICKS + 5) {
            ModRegistry.SCEPTER.use(helper.level, player, InteractionHand.MAIN_HAND) // too late: re-prompts, doesn't commit

            helper.assertTrue(service.ownerOf(key) == player.uuid, "an expired confirm must not unclaim the chunk")
            helper.succeed()
        }
    }

    @GameTest
    fun adminClaimAndUnclaimBypassTheGameplayGates(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        val source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER)
        val dispatcher = helper.level.server.commands.dispatcher
        val service = ClaimDataAccess.of(helper.level.server).service
        val key = ownChunkKey(helper, player)
        // Exhaust the normal allowance first so a gameplay-gated claim would be denied here.
        service.forceClaim(player.uuid, randomChunkKey(helper), 0L)

        dispatcher.execute("empiresmc admin claim", source)
        helper.assertTrue(service.ownerOf(key) == player.uuid, "admin claim must bypass the allowance gate")

        dispatcher.execute("empiresmc admin unclaim", source)
        helper.assertTrue(service.ownerOf(key) == null, "admin unclaim must bypass the cooldown gate")

        helper.succeed()
    }
}
