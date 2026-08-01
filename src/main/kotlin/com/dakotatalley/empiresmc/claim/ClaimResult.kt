/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dakotatalley.empiresmc.claim

sealed class ClaimResult {
    data object Success : ClaimResult()
    data object AlreadyClaimed : ClaimResult()
    data object NotOwner : ClaimResult()
    data object NoAllowance : ClaimResult()
    data class OnCooldown(val remainingTicks: Long) : ClaimResult()
}
