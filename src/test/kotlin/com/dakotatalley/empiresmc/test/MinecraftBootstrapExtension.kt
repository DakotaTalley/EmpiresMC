/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.test

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource

// Registry-backed code throws "Not bootstrapped" (fabric DEV-009) until this runs once per JVM.
// The root-context store (JUnit 5's documented mechanism for one-time global setup) makes the
// bootstrap idempotent across every test class that applies @ExtendWith(MinecraftBootstrapExtension::class),
// without each class needing its own guard.
class MinecraftBootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        context.root.getStore(NAMESPACE).getOrComputeIfAbsent(KEY) {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
            object : CloseableResource {
                override fun close() {}
            }
        }
    }

    companion object {
        private val NAMESPACE = Namespace.create(MinecraftBootstrapExtension::class.java)
        private const val KEY = "bootstrapped"
    }
}
