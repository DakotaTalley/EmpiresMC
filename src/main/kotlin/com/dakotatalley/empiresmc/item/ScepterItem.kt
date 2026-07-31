package com.dakotatalley.empiresmc.item

import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.claim.ClaimGestureService
import com.dakotatalley.empiresmc.claim.ClaimKey
import com.dakotatalley.empiresmc.claim.ClaimResult
import com.dakotatalley.empiresmc.claim.ClaimService
import com.dakotatalley.empiresmc.util.PlayerSounds
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.function.Consumer

// Dev-placeholder Scepter: stick model/texture per the project brief, Epic rarity so the name
// reads as special. A stateless handle by design (no tier/claim data on the stack - see
// ClaimService.EmpireProfile) - losing or duplicating the item never touches progress.
//
// Phase 4 gesture map (fabric SEC-001: everything below is resolved on the logical server; the
// client only ever sends the vanilla use action, nothing is trusted from a client-computed chunk
// or target):
//   Unclaimed chunk        | use        | claim it (if allowance remains)
//   Own claimed chunk      | use        | status readout
//   Own claimed chunk      | sneak-use  | unclaim - two-step confirm, ~5s window
//   Anywhere else          | sneak-use  | reserved for the Phase 8 upgrade flow
class ScepterItem(properties: Properties) : Item(properties) {
    // Instance-scoped, not a companion/object: ModRegistry.SCEPTER is itself a singleton, so this
    // behaves like one without introducing a second top-level singleton for what is purely this
    // item's own transient interaction state (never persisted - see ClaimGestureService).
    private val gestures = ClaimGestureService()

    init {
        // gestures is keyed against Level.gameTime, which restarts near 0 for a new/reloaded
        // single-player world - without this reset, a debounce or pending-unclaim-confirm tick
        // left over from the previous world could wrongly look "still in window" for the new one
        // (QA/QC finding: stale cross-world gesture state).
        ServerLifecycleEvents.SERVER_STARTED.register { gestures.clear() }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverPlayer = player as? ServerPlayer ?: return InteractionResult.SUCCESS

        val tick = level.gameTime
        if (gestures.shouldDebounce(serverPlayer.uuid, tick)) return InteractionResult.SUCCESS

        val service = ClaimDataAccess.of(serverPlayer.level().server).service
        val key = ClaimKey(level.dimension().identifier(), ChunkPos.containing(serverPlayer.blockPosition()))

        if (serverPlayer.isShiftKeyDown) {
            handleSneakUse(serverPlayer, service, key, tick)
        } else {
            handleUse(serverPlayer, service, key, tick)
        }
        return InteractionResult.SUCCESS_SERVER
    }

    private fun handleUse(player: ServerPlayer, service: ClaimService, key: ClaimKey, tick: Long) {
        val owner = service.ownerOf(key)
        when {
            owner == null -> handleClaimAttempt(player, service, key, tick)
            owner == player.uuid -> sendStatus(player, service, key, tick)
            else -> feedback(player, Component.translatable(KEY_OWNED_BY_OTHER), SoundEvents.VILLAGER_NO)
        }
    }

    private fun handleClaimAttempt(player: ServerPlayer, service: ClaimService, key: ClaimKey, tick: Long) {
        when (service.claim(player.uuid, key, tick)) {
            ClaimResult.Success ->
                feedback(player, Component.translatable(KEY_CLAIMED, key.pos.x, key.pos.z), SoundEvents.EXPERIENCE_ORB_PICKUP)
            ClaimResult.NoAllowance ->
                feedback(player, Component.translatable(KEY_NO_ALLOWANCE), SoundEvents.VILLAGER_NO)
            // claim() only ever returns Success/NoAllowance on a chunk this branch already knows
            // is unowned - AlreadyClaimed/NotOwner/OnCooldown belong to unclaim()'s result space.
            ClaimResult.AlreadyClaimed, ClaimResult.NotOwner, is ClaimResult.OnCooldown -> Unit
        }
    }

    private fun sendStatus(player: ServerPlayer, service: ClaimService, key: ClaimKey, tick: Long) {
        // Invariant: only reached when service.ownerOf(key) == player.uuid, so a record must exist.
        val record = service.recordOf(key) ?: return
        val used = service.claimsOf(player.uuid).size
        val allowance = service.allowanceOf(player.uuid)
        val tier = service.profileOf(player.uuid).scepterTier
        val remaining = ClaimService.remainingCooldownTicks(record, tick)

        player.sendSystemMessage(Component.translatable(KEY_STATUS_CHUNK, key.pos.x, key.pos.z))
        player.sendSystemMessage(Component.translatable(KEY_STATUS_ALLOWANCE, used, allowance, tier))
        if (remaining > 0) {
            player.sendSystemMessage(
                cooldownComponent(remaining, KEY_STATUS_COOLDOWN_REMAINING, KEY_STATUS_COOLDOWN_REMAINING_ONE, KEY_STATUS_COOLDOWN_SOON),
            )
        } else {
            player.sendSystemMessage(Component.translatable(KEY_STATUS_COOLDOWN_READY))
        }
        PlayerSounds.playTo(player, SoundEvents.BOOK_PAGE_TURN)
    }

    private fun handleSneakUse(player: ServerPlayer, service: ClaimService, key: ClaimKey, tick: Long) {
        val owner = service.ownerOf(key)
        if (owner != player.uuid) {
            // QA/QC finding: a player who habitually holds sneak lands here on their first attempt
            // to claim (owner == null) and gets no hint to let go - only the unclaimed case gets the
            // extra sentence, since releasing sneak on someone else's claim still wouldn't claim it.
            val reservedKey = if (owner == null) KEY_UPGRADE_RESERVED_UNCLAIMED else KEY_UPGRADE_RESERVED
            feedback(player, Component.translatable(reservedKey), SoundEvents.VILLAGER_NO)
            return
        }
        if (gestures.confirmUnclaim(player.uuid, key, tick)) {
            commitUnclaim(player, service, key, tick)
        } else {
            gestures.requestUnclaimConfirm(player.uuid, key, tick)
            feedback(player, Component.translatable(KEY_UNCLAIM_CONFIRM, key.pos.x, key.pos.z), SoundEvents.LEVER_CLICK)
        }
    }

    private fun commitUnclaim(player: ServerPlayer, service: ClaimService, key: ClaimKey, tick: Long) {
        when (val result = service.unclaim(player.uuid, key, tick)) {
            ClaimResult.Success ->
                feedback(player, Component.translatable(KEY_UNCLAIM_SUCCESS, key.pos.x, key.pos.z), SoundEvents.VILLAGER_YES)
            is ClaimResult.OnCooldown ->
                feedback(
                    player,
                    cooldownComponent(result.remainingTicks, KEY_UNCLAIM_COOLDOWN, KEY_UNCLAIM_COOLDOWN_ONE, KEY_UNCLAIM_COOLDOWN_SOON),
                    SoundEvents.VILLAGER_NO,
                )
            // handleSneakUse already checked ownerOf(key) == player.uuid immediately before this
            // call (single-threaded server, no intervening mutation) - unclaim() cannot return
            // NotOwner here, and never returns AlreadyClaimed/NoAllowance at all.
            ClaimResult.NotOwner, ClaimResult.AlreadyClaimed, ClaimResult.NoAllowance -> Unit
        }
    }

    // Shared by the status readout and the unclaim denial so both switch wording at the same two
    // thresholds: under a minute reads as "soon" (a whole-minute count rounded up from a
    // sub-minute remainder would overstate the wait), and exactly one minute gets its own
    // singular string instead of the grammatically-off "1 more minutes".
    private fun cooldownComponent(remainingTicks: Long, pluralKey: String, singularKey: String, soonKey: String): Component {
        if (remainingTicks < ClaimService.TICKS_PER_MINUTE) return Component.translatable(soonKey)
        val minutes = ClaimService.formatCooldownMinutes(remainingTicks)
        return if (minutes == "1") Component.translatable(singularKey) else Component.translatable(pluralKey, minutes)
    }

    private fun feedback(player: ServerPlayer, message: Component, sound: SoundEvent) {
        player.sendSystemMessage(message)
        PlayerSounds.playTo(player, sound)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipDisplay: TooltipDisplay,
        textConsumer: Consumer<Component>,
        flag: TooltipFlag,
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, textConsumer, flag)
        textConsumer.accept(Component.translatable("item.empiresmc.scepter.tooltip.flavor"))
        // Placeholder until Phase 6's client sync makes real used/total numbers available here.
        textConsumer.accept(Component.translatable("item.empiresmc.scepter.tooltip.chunks_placeholder"))
    }

    companion object {
        private const val KEY_CLAIMED = "empiresmc.claim.claimed"
        private const val KEY_NO_ALLOWANCE = "empiresmc.claim.no_allowance"
        private const val KEY_OWNED_BY_OTHER = "empiresmc.claim.owned_by_other"
        private const val KEY_UPGRADE_RESERVED = "empiresmc.claim.upgrade_reserved"
        private const val KEY_UPGRADE_RESERVED_UNCLAIMED = "empiresmc.claim.upgrade_reserved_unclaimed"
        private const val KEY_STATUS_CHUNK = "empiresmc.claim.status.chunk"
        private const val KEY_STATUS_ALLOWANCE = "empiresmc.claim.status.allowance"
        private const val KEY_STATUS_COOLDOWN_READY = "empiresmc.claim.status.cooldown_ready"
        private const val KEY_STATUS_COOLDOWN_REMAINING = "empiresmc.claim.status.cooldown_remaining"
        private const val KEY_STATUS_COOLDOWN_REMAINING_ONE = "empiresmc.claim.status.cooldown_remaining_one"
        private const val KEY_STATUS_COOLDOWN_SOON = "empiresmc.claim.status.cooldown_remaining_soon"
        private const val KEY_UNCLAIM_CONFIRM = "empiresmc.claim.unclaim_confirm"
        private const val KEY_UNCLAIM_SUCCESS = "empiresmc.claim.unclaim_success"
        private const val KEY_UNCLAIM_COOLDOWN = "empiresmc.claim.unclaim_cooldown"
        private const val KEY_UNCLAIM_COOLDOWN_ONE = "empiresmc.claim.unclaim_cooldown_one"
        private const val KEY_UNCLAIM_COOLDOWN_SOON = "empiresmc.claim.unclaim_cooldown_soon"
    }
}
