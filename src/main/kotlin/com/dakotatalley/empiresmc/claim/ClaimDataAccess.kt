package com.dakotatalley.empiresmc.claim

import com.dakotatalley.empiresmc.EmpiresMC
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer

// Minecraft glue around ClaimData - the only place that knows about MinecraftServer/ServerLevel,
// keeping ClaimService and the data model free of it (fabric DEV-005-adjacent: this is the
// explicit, deterministic attach point instead of relying on incidental first access).
object ClaimDataAccess {
    fun initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::attach)
    }

    fun of(server: MinecraftServer): ClaimData = server.overworld().dataStorage.computeIfAbsent(ClaimData.TYPE)

    private fun attach(server: MinecraftServer) {
        warnAboutUnknownDimensions(server, of(server))
    }

    // A dimension removed from the world/datapack since it was last saved isn't an error - its
    // claims are retained as-is (never dropped) so they come back if the dimension does. This
    // just surfaces that state to an operator instead of it going unnoticed.
    private fun warnAboutUnknownDimensions(server: MinecraftServer, claimData: ClaimData) {
        val claimedDimensions = claimData.claims.keys.map { it.dimension }.toSet()
        for (dimension in claimedDimensions) {
            val key = ResourceKey.create(Registries.DIMENSION, dimension)
            if (server.getLevel(key) == null) {
                EmpiresMC.LOGGER.warn(
                    "Claim data references dimension '{}', which is not currently loaded. Its claims are being retained.",
                    dimension,
                )
            }
        }
    }
}
