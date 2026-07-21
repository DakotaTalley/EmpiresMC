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
