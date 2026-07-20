package com.dakotatalley.empiresmc

import com.dakotatalley.empiresmc.registry.ModRegistry
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EmpiresMC : ModInitializer {
    const val MOD_ID = "empiresmc"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModRegistry.initialize()
        LOGGER.info("EmpiresMC common initializer running")
    }
}
