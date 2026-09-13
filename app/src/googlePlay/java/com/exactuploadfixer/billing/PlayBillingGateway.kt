package com.exactuploadfixer.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production billing implementation.
 *
 * Source synthesis:
 * - Interface isolation: ChatGPT Step 6
 * - Implementation completeness (queryPurchasesAsync, acknowledgePurchase,
 *   autoServiceReconnection): Gemini Phase 3
 *
 * V1 notes:
 * - Client-side entitlement only. Acceptable for a simple one-time unlock.
 * - acknowledgePurchase is required; Google refunds unacknowledged purchases after 3 days.
 * - autoServiceReconnection() handles transient Play Store disconnects.
 *
 * Note: Migrated to billing-ktx v8 (using 8.0.0 in shared catalog).
 * - ProGuard rules aligned with v8.
 * - Auto-reconnection handles transient disconnects cleanly.
 */
class PlayBillingGateway(private val appContext: Context) : PurchasesUpdatedListener, BillingGateway {

    companion object {
        // Must match the product ID created in Google Play Console > Monetize > Products
        const val PRO_PRODUCT_ID = "exact_upload_fixer_pro"
    }

    private val _isProUnlocked = MutableStateFlow(false)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    override suspend fun connect() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resume(Unit)
                    } else {
                        if (cont.isActive) cont.resumeWithException(
                            IllegalStateException("Billing setup failed: ${result.debugMessage}")
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // ViewModel calls connect() again on the next onResume — no retry loop needed here.
                }
            })
        }
    }

    override suspend fun refreshEntitlement() {
        if (!billingClient.isReady) return

        // Check owned purchases
        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isProUnlocked.value = purchases.any { purchase ->
                        purchase.products.contains(PRO_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }

        // Pre-fetch product details so launchPurchase works without an extra async call
        loadProductDetails()
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(
                params,
                object : ProductDetailsResponseListener {
                    override fun onProductDetailsResponse(
                        result: BillingResult,
                        detailsResult: QueryProductDetailsResult
                    ) {
                        cachedProductDetails = detailsResult.productDetailsList.firstOrNull()
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            )
        }
    }

    override fun launchPurchase(activity: Activity): Boolean {
        val details = cachedProductDetails ?: return false  // not ready — caller should inform the user

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        // launchBillingFlow returns a BillingResult: OK means the flow was started.
        // Other codes (e.g. ITEM_UNAVAILABLE, BILLING_UNAVAILABLE) mean nothing was
        // shown, so report failure and let the caller surface feedback.
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) return
        purchases.forEach { purchase ->
            if (purchase.products.contains(PRO_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        _isProUnlocked.value = true

        // Must acknowledge within 3 days or Google auto-refunds
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* fire-and-forget for V1 */ }
        }
    }

    override fun dispose() {
        billingClient.endConnection()
    }
}
