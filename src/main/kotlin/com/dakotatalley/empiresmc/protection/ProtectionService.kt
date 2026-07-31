package com.dakotatalley.empiresmc.protection

import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.claim.ClaimKey
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level

// Minecraft glue around ClaimService for the core "outside your claims you cannot alter the
// world" rule (Phase 5) - mirrors ClaimDataAccess's role as the one place that bridges pure claim
// logic to Minecraft types, so hooks (ProtectionHooks) stay dumb: compute a position, ask here.
object ProtectionService {
    // Split from a single canModify so a future Phase 7 config can gate break/place/interact
    // independently (e.g. enforceBreak/enforcePlace/enforceInteract booleans) without restructuring
    // any hook - each hook already calls a distinct method here, so Phase 7 only touches these
    // three bodies, not call sites.
    fun canBreak(player: Player, level: Level, pos: BlockPos): ProtectionResult = checkOwnership(player, level, pos)

    // Governs literal block placement only.
    fun canPlace(player: Player, level: Level, pos: BlockPos): ProtectionResult = checkOwnership(player, level, pos)

    // Governs the modifying-interaction deny-list (bucket/flint&steel/hoe/shovel/axe/bone meal) -
    // shares the UseBlockCallback hook with canPlace, but is a distinct predicate so enforcement of
    // modifying-item use and raw placement can diverge later instead of being forced to move
    // together.
    fun canInteract(player: Player, level: Level, pos: BlockPos): ProtectionResult = checkOwnership(player, level, pos)

    // Own-claim only - claims don't protect land *from* others, they define where the acting
    // player may build, so DeniedWild and DeniedOwnedByOther both block identically. The split
    // exists purely so ProtectionFeedback can pick a more useful message, not to change
    // enforcement.
    private fun checkOwnership(player: Player, level: Level, pos: BlockPos): ProtectionResult {
        if (level.isClientSide) return ProtectionResult.Allowed
        if (player.isCreative) return ProtectionResult.Allowed
        val server = (level as ServerLevel).server
        val key = ClaimKey(level.dimension().identifier(), ChunkPos.containing(pos))
        val owner = ClaimDataAccess.of(server).service.ownerOf(key)
        return when {
            owner == player.uuid -> ProtectionResult.Allowed
            owner == null -> ProtectionResult.DeniedWild
            else -> ProtectionResult.DeniedOwnedByOther
        }
    }
}
