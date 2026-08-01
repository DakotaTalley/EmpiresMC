/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc

import com.dakotatalley.empiresmc.registry.ModRegistry
import com.dakotatalley.empiresmc.test.MinecraftBootstrapExtension
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MinecraftBootstrapExtension::class)
class EmpiresMCTest {

    @Test
    fun modIdIsValidFabricId() {
        assertTrue(EmpiresMC.MOD_ID.matches(Regex("^[a-z][a-z0-9-_]*$")))
    }

    @Test
    fun registryBootstrapsAgainstRealMinecraftRegistries() {
        // Proves the bootstrap extension actually wired up vanilla registries, not just that our no-op didn't throw.
        assertNotNull(Items.STONE)
        ModRegistry.initialize()
    }
}
