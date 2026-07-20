package com.dakotatalley.empiresmc.registry

// Empty for now, but still called explicitly: static fields on this object only register
// once the class is initialized, and that only happens on first access (fabric DEV-005).
object ModRegistry {
    fun initialize() {
    }
}
