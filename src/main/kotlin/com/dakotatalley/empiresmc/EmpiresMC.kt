package com.dakotatalley.empiresmc

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EmpiresMC : ModInitializer {
    const val MOD_ID = "empiresmc"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("EmpiresMC common initializer running")
    }
}
