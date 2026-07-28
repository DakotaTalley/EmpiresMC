package com.dakotatalley.empiresmc.protection

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.FlintAndSteelItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

// Registers the Fabric API events that enforce Phase 5's core rule. Every listener is dumb: compute
// a position (or two), ask ProtectionService, deny via ProtectionFeedback + a non-PASS result.
object ProtectionHooks {
    fun initialize() {
        // Authoritative break cancel - covers every break path, not just the click-driven one below.
        PlayerBlockBreakEvents.BEFORE.register(
            PlayerBlockBreakEvents.Before { level, player, pos, _, _ -> handleBreak(level, player, pos) },
        )

        // Fires on the initial left-click, before crack progress starts - stops the block from even
        // beginning to crack, on top of the authoritative BEFORE cancel above.
        AttackBlockCallback.EVENT.register(
            AttackBlockCallback { player, level, _, pos, _ ->
                if (handleBreak(level, player, pos)) InteractionResult.PASS else InteractionResult.FAIL
            },
        )

        // Fires at the head of ServerPlayerGameMode.useItemOn, before vanilla's own block-use,
        // modifying-interaction, and placement dispatch - canceling here (non-PASS) pre-empts all
        // three, so this one hook gates the modifying-interaction deny-list and placement together.
        UseBlockCallback.EVENT.register(
            UseBlockCallback { player, level, hand, hit ->
                val result = evaluateUseBlock(player, level, hand, hit)
                if (result == ProtectionResult.Allowed) {
                    InteractionResult.PASS
                } else {
                    (player as? ServerPlayer)?.let { ProtectionFeedback.deny(it, level, result) }
                    InteractionResult.FAIL
                }
            },
        )
    }

    private fun handleBreak(level: Level, player: Player, pos: BlockPos): Boolean {
        val result = ProtectionService.canBreak(player, level, pos)
        if (result != ProtectionResult.Allowed) {
            (player as? ServerPlayer)?.let { ProtectionFeedback.deny(it, level, result) }
        }
        return result == ProtectionResult.Allowed
    }

    private fun evaluateUseBlock(player: Player, level: Level, hand: InteractionHand, hit: BlockHitResult): ProtectionResult {
        val stack = player.getItemInHand(hand)
        val item = stack.item
        return when {
            isModifyingItem(item) -> checkInteract(player, level, item, hit)
            item is BlockItem -> checkPlacement(player, level, hand, stack, hit)
            else -> ProtectionResult.Allowed
        }
    }

    // Class-based, not item ID/tag, so a modded item reusing one of these vanilla classes is
    // covered automatically.
    private fun isModifyingItem(item: Item): Boolean =
        item is BucketItem || item is FlintAndSteelItem || item is HoeItem || item is ShovelItem || item is AxeItem || item is BoneMealItem

    // Hoe/shovel/axe/bone meal transform only the clicked block. Bucket/flint & steel can affect
    // either the clicked block (drain source, ignite target) or the adjacent one (place fluid on a
    // non-replaceable target, ignite a face) depending on target state - checking both never
    // under-denies at a claim/wild border.
    private fun checkInteract(player: Player, level: Level, item: Item, hit: BlockHitResult): ProtectionResult {
        val primary = ProtectionService.canInteract(player, level, hit.blockPos)
        if (primary != ProtectionResult.Allowed) return primary
        if (item is BucketItem || item is FlintAndSteelItem) {
            return ProtectionService.canInteract(player, level, hit.blockPos.relative(hit.direction))
        }
        return ProtectionResult.Allowed
    }

    // Checks the real placement position (built the same way vanilla would, via BlockPlaceContext -
    // not hit.blockPos, since vanilla adjusts for click side/replaceability) and, for multi-block
    // placements, the companion half too, so a placement straddling a chunk border is denied
    // outright rather than placing one half and silently dropping the other.
    private fun checkPlacement(player: Player, level: Level, hand: InteractionHand, stack: ItemStack, hit: BlockHitResult): ProtectionResult {
        val context = BlockPlaceContext(player, hand, stack, hit)
        val primary = ProtectionService.canPlace(player, level, context.clickedPos)
        if (primary != ProtectionResult.Allowed) return primary
        val block = (stack.item as BlockItem).block
        val placementState = block.getStateForPlacement(context) ?: return ProtectionResult.Allowed
        val companion = companionPos(context, placementState) ?: return ProtectionResult.Allowed
        return ProtectionService.canPlace(player, level, companion)
    }

    // Doors' second half is a pure Y+1 offset - ChunkPos is X/Z-only, so it can never actually cross
    // a chunk border; kept for symmetry/defense-in-depth. Beds are the genuine cross-border case
    // (horizontal offset via getConnectedDirection).
    private fun companionPos(context: BlockPlaceContext, state: BlockState): BlockPos? = when {
        state.block is BedBlock -> context.clickedPos.relative(BedBlock.getConnectedDirection(state))
        state.block is DoorBlock -> context.clickedPos.above()
        else -> null
    }
}
