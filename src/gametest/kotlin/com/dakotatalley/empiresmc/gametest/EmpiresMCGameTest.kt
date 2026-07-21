package com.dakotatalley.empiresmc.gametest

import com.dakotatalley.empiresmc.EmpiresMC
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
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.gamerules.GameRules

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
}
