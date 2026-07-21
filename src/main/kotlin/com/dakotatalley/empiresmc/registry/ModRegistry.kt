package com.dakotatalley.empiresmc.registry

import com.dakotatalley.empiresmc.EmpiresMC
import com.dakotatalley.empiresmc.item.ScepterItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity

// static final fields on this object only register once the class is initialized, and that only
// happens on first access (fabric DEV-005) - initialize() is the explicit, deterministic trigger.
object ModRegistry {
    private val SCEPTER_ID: ResourceKey<Item> = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(EmpiresMC.MOD_ID, "scepter"),
    )

    // maxCount 1 (no stacking - keeps tooltip/HUD semantics simple), Epic rarity, fireResistant()
    // both marks the item (and any dropped ItemEntity of it) immune to fire/lava damage and
    // populates the DAMAGE_RESISTANT data component ItemStack.canBeHurtBy checks.
    val SCEPTER: ScepterItem = Registry.register(
        BuiltInRegistries.ITEM,
        SCEPTER_ID,
        ScepterItem(
            Item.Properties()
                .setId(SCEPTER_ID)
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant(),
        ),
    )

    fun initialize() {
    }
}
