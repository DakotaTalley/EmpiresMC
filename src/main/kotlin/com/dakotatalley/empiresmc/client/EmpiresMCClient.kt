/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.client

import com.dakotatalley.empiresmc.EmpiresMC
import net.fabricmc.api.ClientModInitializer

object EmpiresMCClient : ClientModInitializer {
    override fun onInitializeClient() {
        EmpiresMC.LOGGER.info("EmpiresMC client initializer running")
    }
}
