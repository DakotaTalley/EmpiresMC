package com.dakotatalley.empiresmc.protection

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.ItemEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.FlintAndSteelItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

// Registers the Fabric API events that enforce Phase 5's core rule. Every listener is dumb: compute
// a position (or two), ask ProtectionService, deny via ProtectionFeedback + a non-PASS/non-null result.
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

        // Wraps Item.useOn(UseOnContext) - the call ServerPlayerGameMode.useItemOn makes only *after*
        // the clicked block's own reaction (door/chest/button toggling, opening a menu, ...) has
        // already had first refusal and declined to consume the interaction. Returning a non-null
        // result here therefore never preempts a pure interaction - it only fires for the item
        // dispatch vanilla itself was about to run, so a player holding a placeable/modifying item
        // can still open a door or chest outside their claim exactly like vanilla would. (Gating this
        // instead via UseBlockCallback, which fires at the very head of useItemOn before the block
        // gets a say, was tried first and wrongly denied opening doors/chests whenever the held item
        // was a deny-list item or BlockItem - confirmed via manual playtest, root-caused by
        // decompiling ServerPlayerGameMode.useItemOn's actual call order.)
        ItemEvents.USE_ON.register(
            ItemEvents.UseOnCallback { context ->
                val result = evaluateUseOn(context)
                if (result == ProtectionResult.Allowed) {
                    null
                } else {
                    (context.player as? ServerPlayer)?.let { ProtectionFeedback.deny(it, context.level, result) }
                    InteractionResult.FAIL
                }
            },
        )

        // Buckets never reach Item.useOn in any meaningful way: BucketItem overrides the generic
        // Item.use(Level, Player, InteractionHand) instead and performs its own raycast internally,
        // a separate dispatch path (ServerPlayerGameMode.useItem, reached via a follow-up client
        // packet whenever useItemOn didn't consume the interaction) that ItemEvents.USE_ON cannot
        // see. UseItemCallback wraps Item.use(...) at the head of that method, before the raycast
        // runs, so we replicate BucketItem's own raycast here to know what it's about to affect.
        UseItemCallback.EVENT.register(
            UseItemCallback { player, level, hand ->
                val stack = player.getItemInHand(hand)
                val bucket = stack.item as? BucketItem ?: return@UseItemCallback InteractionResult.PASS
                val hit = bucketTarget(player, level, bucket) ?: return@UseItemCallback InteractionResult.PASS
                val result = checkInteractPositions(player, level, hit.blockPos, hit.blockPos.relative(hit.direction))
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

    // context.player is nullable (a dispenser can drive a UseOnContext with no player); that's
    // outside Phase 5's scope entirely ("player cannot alter the world"), so it's simply allowed.
    private fun evaluateUseOn(context: UseOnContext): ProtectionResult {
        val player = context.player ?: return ProtectionResult.Allowed
        val item = context.itemInHand.item
        return when {
            isModifyingItem(item) -> checkInteract(player, context, item)
            item is BlockItem -> checkPlacement(player, context)
            else -> ProtectionResult.Allowed
        }
    }

    // Class-based, not item ID/tag, so a modded item reusing one of these vanilla classes is
    // covered automatically. Bucket is deliberately excluded here - see the UseItemCallback
    // registration above for why it can't be gated at this call site.
    private fun isModifyingItem(item: Item): Boolean =
        item is FlintAndSteelItem || item is HoeItem || item is ShovelItem || item is AxeItem || item is BoneMealItem

    // Hoe/shovel/axe/bone meal transform only the clicked block. Flint & steel can affect either the
    // clicked block (ignite the target itself) or the adjacent face (ignite a face) depending on
    // target state - checking both never under-denies at a claim/wild border.
    private fun checkInteract(player: Player, context: UseOnContext, item: Item): ProtectionResult {
        val companion = if (item is FlintAndSteelItem) context.clickedPos.relative(context.clickedFace) else null
        return checkInteractPositions(player, context.level, context.clickedPos, companion)
    }

    private fun checkInteractPositions(player: Player, level: Level, primary: BlockPos, companion: BlockPos?): ProtectionResult {
        val result = ProtectionService.canInteract(player, level, primary)
        if (result != ProtectionResult.Allowed || companion == null) return result
        return ProtectionService.canInteract(player, level, companion)
    }

    // Checks the real placement position (built the same way vanilla would, via BlockPlaceContext -
    // not the clicked block's position, since vanilla adjusts for click side/replaceability) and, for
    // multi-block placements, the companion half too, so a placement straddling a chunk border is
    // denied outright rather than placing one half and silently dropping the other.
    private fun checkPlacement(player: Player, context: UseOnContext): ProtectionResult {
        val placeContext = BlockPlaceContext(context)
        val primary = ProtectionService.canPlace(player, placeContext.level, placeContext.clickedPos)
        if (primary != ProtectionResult.Allowed) return primary
        val block = (context.itemInHand.item as BlockItem).block
        val placementState = block.getStateForPlacement(placeContext) ?: return ProtectionResult.Allowed
        val companion = companionPos(placeContext, placementState) ?: return ProtectionResult.Allowed
        return ProtectionService.canPlace(player, placeContext.level, companion)
    }

    // Doors' second half is a pure Y+1 offset - ChunkPos is X/Z-only, so it can never actually cross
    // a chunk border; kept for symmetry/defense-in-depth. Beds are the genuine cross-border case
    // (horizontal offset via getConnectedDirection).
    private fun companionPos(context: BlockPlaceContext, state: BlockState): BlockPos? = when {
        state.block is BedBlock -> context.clickedPos.relative(BedBlock.getConnectedDirection(state))
        state.block is DoorBlock -> context.clickedPos.above()
        else -> null
    }

    // Replicates BucketItem's own getPlayerPOVHitResult raycast (protected on Item, not visible from
    // here) via the same public pieces it's built from - an empty bucket clips through non-source
    // fluid to find a source to drain, a full one ignores fluids entirely and only cares about the
    // solid block it's aiming at.
    private fun bucketTarget(player: Player, level: Level, bucket: BucketItem): BlockHitResult? {
        val eye = player.eyePosition
        val to = eye.add(player.calculateViewVector(player.xRot, player.yRot).scale(player.blockInteractionRange()))
        val hit = level.clip(ClipContext(eye, to, ClipContext.Block.OUTLINE, bucket.fluidContext, player))
        return if (hit.type == HitResult.Type.BLOCK) hit else null
    }
}
