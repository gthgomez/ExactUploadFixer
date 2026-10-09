package com.exactuploadfixer.fakes

import android.app.Activity
import com.exactuploadfixer.billing.BillingGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [BillingGateway]. Exposes control methods to simulate
 * purchase and revocation events without a real Play Billing connection.
 * Lives in src/test/ only — never shipped in production APK.
 */
class FakeBillingGateway : BillingGateway {

    private val _isProUnlocked = MutableStateFlow(false)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private val _pendingPurchase = MutableStateFlow(false)
    override val pendingPurchase: StateFlow<Boolean> = _pendingPurchase.asStateFlow()

    private val _priceLabel = MutableStateFlow<String?>(null)
    override val priceLabel: StateFlow<String?> = _priceLabel.asStateFlow()

    /** Test hooks for pending/price state propagation. */
    fun simulatePendingPurchase() {
        _pendingPurchase.value = true
    }

    fun simulatePriceLoaded(label: String) {
        _priceLabel.value = label
    }

    var connectCallCount = 0
    var refreshCallCount = 0

    /** Simulates a successful purchase completing. */
    fun simulatePurchase() {
        _isProUnlocked.value = true
    }

    /** Simulates entitlement being revoked (e.g. refund, subscription lapse). */
    fun simulateRevoke() {
        _isProUnlocked.value = false
    }

    override suspend fun connect() {
        connectCallCount++
    }

    override suspend fun refreshEntitlement() {
        refreshCallCount++
    }

    /** In tests, launchPurchase is a no-op — drive entitlement via simulatePurchase(). */
    override fun launchPurchase(activity: Activity): Boolean = true

    override fun dispose() = Unit
}
