package com.dakotatalley.empiresmc.claim

sealed class ClaimResult {
    data object Success : ClaimResult()
    data object AlreadyClaimed : ClaimResult()
    data object NotOwner : ClaimResult()
    data object NoAllowance : ClaimResult()
}
