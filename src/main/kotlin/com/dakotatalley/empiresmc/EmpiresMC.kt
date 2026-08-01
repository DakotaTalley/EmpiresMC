/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc

import com.dakotatalley.empiresmc.claim.ClaimDataAccess
import com.dakotatalley.empiresmc.command.AdminCommand
import com.dakotatalley.empiresmc.item.ScepterGrant
import com.dakotatalley.empiresmc.protection.ProtectionFeedback
import com.dakotatalley.empiresmc.protection.ProtectionHooks
import com.dakotatalley.empiresmc.registry.ModRegistry
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EmpiresMC : ModInitializer {
    const val MOD_ID = "empiresmc"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModRegistry.initialize()
        ClaimDataAccess.initialize()
        AdminCommand.initialize()
        ScepterGrant.initialize()
        ProtectionFeedback.initialize()
        ProtectionHooks.initialize()
        LOGGER.info("EmpiresMC common initializer running")
    }
}
