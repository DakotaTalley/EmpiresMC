package com.dakotatalley.empiresmc.gametest

import com.dakotatalley.empiresmc.EmpiresMC
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.gametest.framework.GameTestHelper

class EmpiresMCGameTest {
    @GameTest
    fun modIsLoaded(helper: GameTestHelper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded(EmpiresMC.MOD_ID), "EmpiresMC should be loaded")
        helper.succeed()
    }
}
