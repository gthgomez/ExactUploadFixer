package com.exactuploadfixer.billing

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchaseResult
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Package as RcPackage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Amazon Appstore billing implementation backed by RevenueCat SDK v9+.
 *
 * Satisfies the [BillingGateway] interface — ViewModel and UI have zero knowledge
 * of RevenueCat or Amazon IAP. The billing isolation contract is preserved.
 *
 * Structural parity with PlayBillingGateway:
 *   connect()            → configure RC (idempotent via Purchases.isConfigured guard)
 *   refreshEntitlement() → awaitCustomerInfo() + awaitOfferings() (mirrors queryPurchasesAsync + queryProductDetailsAsync)
 *   launchPurchase()     → awaitPurchaseResult() launched on MainScope (mirrors launchBillingFlow)
 *   dispose()            → cancel MainScope; RC manages its own connection lifecycle
 *
 * Amazon IAP contracts handled by RevenueCat internally:
 *   - notifyFulfillment(FULFILLED) — RC calls this after a successful purchase
 *   - getPurchaseUpdates restore  — RC calls this as part of awaitCustomerInfo()
 *
 * Configuration: see BillingConfig.kt — replace placeholder values before release.
 */
class RevenueCatBillingGateway(private val appContext: Context) : BillingGateway {

    private val _isProUnlocked = MutableStateFlow(false)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    /** Cached during refreshEntitlement() — required before launchPurchase() works. */
    private var cachedProPackage: RcPackage? = null

    /** Scope for launchPurchase() coroutines. Cancelled in dispose(). */
    private val scope = MainScope()

    /**
     * Configure RevenueCat SDK for Amazon IAP.
     * Idempotent — safe to call on every onResume (existing ViewModel contract).
     */
    override suspend fun connect() {
        if (Purchases.isConfigured) return
        Purchases.configure(
            AmazonConfiguration.Builder(appContext, BillingConfig.REVENUECAT_API_KEY).build()
        )
    }

    /**
     * Fetch current entitlement state and pre-cache the Pro package.
     * Called on every connect() and every onResume (ViewModel contract).
     * Errors leave _isProUnlocked unchanged — free tier continues offline.
     */
    override suspend fun refreshEntitlement() {
        if (!Purchases.isConfigured) return
        refreshCustomerInfo()
        fetchProPackage()
    }

    private suspend fun refreshCustomerInfo() {
        try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            _isProUnlocked.value =
                customerInfo.entitlements[BillingConfig.ENTITLEMENT_PRO]?.isActive == true
        } catch (_: PurchasesException) {
            // Network or RC error — keep current state, free tier continues
        }
    }

    private suspend fun fetchProPackage() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            cachedProPackage = offerings.current?.availablePackages?.firstOrNull { pkg ->
                pkg.product.id in BillingConfig.AMAZON_PRODUCT_IDS
            }
        } catch (_: PurchasesException) {
            // Error — cachedProPackage stays null; launchPurchase() will no-op safely
        }
    }

    /**
     * Launch the Amazon IAP purchase flow for the Pro unlock.
     * No-op if cachedProPackage is null — offerings not yet loaded. The user
     * can tap again after the app retries refreshEntitlement() on next onResume.
     */
    override fun launchPurchase(activity: Activity) {
        val pkg = cachedProPackage ?: return
        scope.launch {
            val result = Purchases.sharedInstance.awaitPurchaseResult(
                PurchaseParams.Builder(activity, pkg).build()
            )
            result.getOrNull()?.let { purchaseResult ->
                _isProUnlocked.value =
                    purchaseResult.customerInfo.entitlements[BillingConfig.ENTITLEMENT_PRO]
                        ?.isActive == true
            }
            // null result = user cancelled or IAP error — isProUnlocked unchanged
        }
    }

    /**
     * Cancel the purchase coroutine scope.
     * RC manages its own connection lifecycle — no endConnection equivalent.
     */
    override fun dispose() {
        scope.cancel()
    }
}
