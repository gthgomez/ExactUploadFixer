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
    override fun launchPurchase(activity: Activity) = Unit

    override fun dispose() = Unit
}
