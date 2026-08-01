/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.item

import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.registry.ModRegistry
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

// The Scepter is a stateless handle - all empire state lives server-side keyed by the holder's UUID
// (see ClaimService.EmpireProfile), so "you can never lose it" is delivered by re-granting a fresh
// handle rather than by making the item physically indestructible. That keeps the whole feature in
// two plain Fabric API events, no mixins:
//   - JOIN grants the first Scepter exactly once (the receivedScepter flag stops re-grant spam).
//   - AFTER_RESPAWN restores one after a death that dropped it. That is the only respawn path where
//     vanilla doesn't already carry the inventory across: ServerPlayer.restoreFrom copies the old
//     inventory only when keepInventory is on (or when returning from the End), so a normal
//     keepInventory-off death otherwise comes back empty-handed.
object ScepterGrant {
    fun initialize() {
        ServerPlayerEvents.JOIN.register(
            ServerPlayerEvents.Join { player -> grantOnFirstJoin(player) },
        )
        ServerPlayerEvents.AFTER_RESPAWN.register(
            ServerPlayerEvents.AfterRespawn { _, newPlayer, _ -> restoreAfterRespawn(newPlayer) },
        )
    }

    private fun grantOnFirstJoin(player: ServerPlayer) {
        val service = ClaimDataAccess.of(player.level().server).service
        if (service.grantScepterIfNeeded(player.uuid)) {
            // placeItemBackInInventory (not add): the grant flag is already committed at this point,
            // so a full inventory must not silently swallow the item - drop it at the player's feet
            // instead. A brand-new player is empty, but an existing player on a newly-modded world
            // could join full and would otherwise never get their one-time Scepter.
            player.inventory.placeItemBackInInventory(ItemStack(ModRegistry.SCEPTER))
            player.sendSystemMessage(Component.translatable("empiresmc.claim.first_join_instruction"))
        }
    }

    // Guarded purely on "entitled (received one before) AND currently holding none". That makes it a
    // no-op on the respawn paths where vanilla already copied the inventory across (keepInventory on,
    // or returning from the End) and fires only on a keepInventory-off death that actually dropped
    // it. Duplication is harmless by design, so no attempt is made to reclaim the dropped copy - it
    // despawns like ordinary loot while the player already holds a fresh handle.
    private fun restoreAfterRespawn(player: ServerPlayer) {
        val service = ClaimDataAccess.of(player.level().server).service
        if (!service.profileOf(player.uuid).receivedScepter) return
        if (playerHasScepter(player)) return
        player.inventory.add(ItemStack(ModRegistry.SCEPTER))
    }

    private fun playerHasScepter(player: ServerPlayer): Boolean {
        for (slot in 0 until player.inventory.containerSize) {
            if (player.inventory.getItem(slot).item === ModRegistry.SCEPTER) return true
        }
        return false
    }
}
