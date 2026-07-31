package com.dakotatalley.empiresmc.protection

sealed class ProtectionResult {
    data object Allowed : ProtectionResult()
    data object DeniedWild : ProtectionResult()
    data object DeniedOwnedByOther : ProtectionResult()
}
