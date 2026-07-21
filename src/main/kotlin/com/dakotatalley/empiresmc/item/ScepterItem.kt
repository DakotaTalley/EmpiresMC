package com.dakotatalley.empiresmc.item

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.function.Consumer

// Dev-placeholder Scepter: stick model/texture per the project brief, Epic rarity so the name
// reads as special. A stateless handle by design (no tier/claim data on the stack - see
// ClaimService.EmpireProfile) - losing or duplicating the item never touches progress.
class ScepterItem(properties: Properties) : Item(properties) {
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
}
