package com.dakotatalley.empiresmc.gametest

import com.dakotatalley.empiresmc.EmpiresMC
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.permissions.LevelBasedPermissionSet

class EmpiresMCGameTest {
    @GameTest
    fun modIsLoaded(helper: GameTestHelper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded(EmpiresMC.MOD_ID), "EmpiresMC should be loaded")
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
        helper.succeed()
    }
}
