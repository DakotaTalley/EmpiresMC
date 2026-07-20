package com.dakotatalley.empiresmc.client

import com.dakotatalley.empiresmc.EmpiresMC
import net.fabricmc.api.ClientModInitializer

object EmpiresMCClient : ClientModInitializer {
    override fun onInitializeClient() {
        EmpiresMC.LOGGER.info("EmpiresMC client initializer running")
    }
}
