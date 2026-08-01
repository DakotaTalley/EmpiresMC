/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.command

import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.claim.ClaimKey
import com.dakotatalley.empiresmc.registry.ModRegistry
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos

// The permission-level-2 (GAMEMASTERS) admin/debug command skeleton this phase's design decisions
// call for - the dev-loop tool later phases lean on to inspect claim state without a client UI.
object AdminCommand {
    fun initialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> register(dispatcher) }
    }

    private fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("empiresmc")
                .then(
                    Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("claiminfo").executes(::claimInfo))
                        .then(
                            Commands.literal("profile")
                                .then(Commands.argument("player", EntityArgument.player()).executes(::profile))
                        )
                        .then(Commands.literal("scepter").executes(::scepter))
                        .then(
                            Commands.literal("claim")
                                .executes { context -> forceClaim(context, context.source.playerOrException) }
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes { context -> forceClaim(context, EntityArgument.getPlayer(context, "player")) }
                                )
                        )
                        .then(Commands.literal("unclaim").executes(::forceUnclaim))
                )
        )
    }

    private fun claimInfo(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val key = ClaimKey(source.level.dimension().identifier(), ChunkPos.containing(player.blockPosition()))
        val owner = ClaimDataAccess.of(source.server).service.ownerOf(key)

        val message = if (owner == null) {
            "Chunk [${key.pos.x}, ${key.pos.z}] in ${key.dimension} is unclaimed."
        } else {
            "Chunk [${key.pos.x}, ${key.pos.z}] in ${key.dimension} is claimed by $owner."
        }
        source.sendSuccess({ Component.literal(message) }, false)
        return Command.SINGLE_SUCCESS
    }

    private fun profile(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val target = EntityArgument.getPlayer(context, "player")
        val service = ClaimDataAccess.of(source.server).service
        val empireProfile = service.profileOf(target.uuid)
        val used = service.claimsOf(target.uuid).size
        val allowance = service.allowanceOf(target.uuid)

        val message = "${target.scoreboardName}: tier ${empireProfile.scepterTier}, " +
            "scepter ${if (empireProfile.receivedScepter) "received" else "not received"}, " +
            "$used/$allowance chunks claimed."
        source.sendSuccess({ Component.literal(message) }, false)
        return Command.SINGLE_SUCCESS
    }

    // Raw seeding/override tool (Phase 4 design decisions): acts on the chunk under the invoking
    // player, bypassing the Scepter gesture, allowance, and cooldown gates entirely - the
    // mechanism that makes the manual "create a claim, save, reload, observe" flow from Phase 2's
    // exit criteria reproducible from this phase on. Optional trailing <player> attributes the
    // claim to someone else (for future multiplayer testing); the chunk is always the invoker's.
    private fun forceClaim(context: CommandContext<CommandSourceStack>, target: ServerPlayer): Int {
        val source = context.source
        val invoker = source.playerOrException
        val key = ClaimKey(source.level.dimension().identifier(), ChunkPos.containing(invoker.blockPosition()))
        ClaimDataAccess.of(source.server).service.forceClaim(target.uuid, key, source.level.gameTime)
        source.sendSuccess(
            { Component.literal("Force-claimed chunk [${key.pos.x}, ${key.pos.z}] in ${key.dimension} for ${target.scoreboardName}.") },
            false,
        )
        return Command.SINGLE_SUCCESS
    }

    private fun forceUnclaim(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val key = ClaimKey(source.level.dimension().identifier(), ChunkPos.containing(player.blockPosition()))
        val removed = ClaimDataAccess.of(source.server).service.forceUnclaim(key)
        val message = if (removed) {
            "Force-unclaimed chunk [${key.pos.x}, ${key.pos.z}] in ${key.dimension}."
        } else {
            "Chunk [${key.pos.x}, ${key.pos.z}] in ${key.dimension} was not claimed."
        }
        source.sendSuccess({ Component.literal(message) }, false)
        return Command.SINGLE_SUCCESS
    }

    // Recovery of last resort (alongside the crafting recipe): always available, no eligibility
    // check. Harmless to run twice by design - the Scepter is a stateless handle (Phase 3).
    private fun scepter(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        // Drop at feet if the inventory is full rather than silently discarding the Scepter - the
        // success message below would otherwise lie about a recovery that didn't happen.
        player.inventory.placeItemBackInInventory(ItemStack(ModRegistry.SCEPTER))
        source.sendSuccess({ Component.literal("Gave a Scepter to ${player.scoreboardName}.") }, false)
        return Command.SINGLE_SUCCESS
    }
}
