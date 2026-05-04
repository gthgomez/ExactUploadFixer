package com.exactuploadfixer.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Billing isolation contract. ViewModel depends only on this interface.
 * The image engine never imports anything from this package.
 *
 * ViewModel exposes only two things to the UI:
 *   - isProUnlocked (from this StateFlow)
 *   - buyPro(activity) (calls launchPurchase)
 *
 * If billing is unavailable (offline, Play Store absent), the free
 * manual tier must continue to function. isProUnlocked defaults to false.
 *
 * Source: ChatGPT Step 6 isolation pattern.
 */
interface BillingGateway {
    val isProUnlocked: StateFlow<Boolean>

    /** Connect to Play Billing. Safe to call multiple times. */
    suspend fun connect()

    /**
     * Query owned purchases and update isProUnlocked.
     * Also pre-fetches product details so launchPurchase has them cached.
     */
    suspend fun refreshEntitlement()

    /** Launch the Play purchase flow. No-op if product details aren't cached yet. */
    fun launchPurchase(activity: Activity)

    /** End the BillingClient connection. Call from ViewModel.onCleared(). */
    fun dispose()
}
