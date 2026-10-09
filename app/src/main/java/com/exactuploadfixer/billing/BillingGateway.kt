package com.exactuploadfixer.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Billing isolation contract. ViewModel depends only on this interface.
 * The image engine never imports anything from this package.
 *
 * ViewModel exposes only billing state to the UI:
 *   - isProUnlocked (from this StateFlow)
 *   - pendingPurchase (a store-side payment is still processing — no unlock yet)
 *   - priceLabel (store-localized lifetime price, null until loaded)
 *   - buyPro(activity) (calls launchPurchase)
 *
 * If billing is unavailable (offline, Play Store absent), the free
 * manual tier must continue to function. isProUnlocked defaults to false.
 *
 * Source: ChatGPT Step 6 isolation pattern.
 */
interface BillingGateway {
    val isProUnlocked: StateFlow<Boolean>

    /**
     * True while a purchase exists but is in the store's PENDING state
     * (e.g. cash payment awaiting completion). The app must not unlock Pro,
     * and the UI should tell the user the purchase is not finished.
     */
    val pendingPurchase: StateFlow<Boolean>

    /**
     * Store-provided localized price for the lifetime unlock (e.g. "$2.99"),
     * or null while product details are unavailable. UI must never invent a price.
     */
    val priceLabel: StateFlow<String?>

    /** Connect to Play Billing. Safe to call multiple times. */
    suspend fun connect()

    /**
     * Query owned purchases and update isProUnlocked.
     * Also acknowledges any unacknowledged owned purchases (required within 3
     * days or Google auto-refunds) and pre-fetches product details so
     * launchPurchase has them cached.
     */
    suspend fun refreshEntitlement()

    /**
     * Launch the store purchase flow. Returns false when the purchase could not
     * be started (e.g. product details not yet cached, store unavailable) so the
     * caller can surface feedback instead of silently doing nothing.
     */
    fun launchPurchase(activity: Activity): Boolean

    /** End the BillingClient connection. Call from ViewModel.onCleared(). */
    fun dispose()
}
