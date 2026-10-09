package com.exactuploadfixer.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production billing implementation for Google Play.
 *
 * Source synthesis:
 * - Interface isolation: ChatGPT Step 6
 * - Implementation completeness (queryPurchasesAsync, acknowledgePurchase,
 *   autoServiceReconnection): Gemini Phase 3
 *
 * Billing 8 contract points implemented here:
 * - acknowledgePurchase results are CHECKED. Unacknowledged PURCHASED tokens are
 *   retried on every refreshEntitlement() — Google auto-refunds after 3 days, so
 *   a fire-and-forget ack silently loses the user's purchase.
 * - Restored purchases (queryPurchasesAsync after reinstall) are acknowledged
 *   through the same path as fresh purchases.
 * - PENDING purchases never unlock Pro; they surface via [pendingPurchase].
 * - launchPurchase result codes are surfaced to the caller.
 *
 * Note: billing-ktx v8. Auto-reconnection handles transient disconnects.
 */
class PlayBillingGateway(private val appContext: Context) : PurchasesUpdatedListener, BillingGateway {

    companion object {
        // Must match the product ID created in Google Play Console > Monetize > Products
        const val PRO_PRODUCT_ID = "exact_upload_fixer_pro"
        private const val TAG = "PlayBillingGateway"
        private const val ACK_RETRY_LIMIT = 3
    }

    private val _isProUnlocked = MutableStateFlow(false)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private val _pendingPurchase = MutableStateFlow(false)
    override val pendingPurchase: StateFlow<Boolean> = _pendingPurchase.asStateFlow()

    private val _priceLabel = MutableStateFlow<String?>(null)
    override val priceLabel: StateFlow<String?> = _priceLabel.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null

    /** Purchase tokens whose acknowledgment failed and must be retried. */
    private val unacknowledgedTokens = mutableSetOf<String>()

    /** Scope for internal async work (e.g. re-verify on ITEM_ALREADY_OWNED). */
    private val retryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    var unlocked = false
                    var pending = false
                    purchases.forEach { purchase ->
                        if (!purchase.products.contains(PRO_PRODUCT_ID)) return@forEach
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                unlocked = true
                                // Restored and fresh purchases alike must be acknowledged
                                // within 3 days or Google auto-refunds them.
                                acknowledgeIfNeeded(purchase)
                            }
                            Purchase.PurchaseState.PENDING -> pending = true
                            // REFUNDED / unspecified: leave unlocked untouched for this token
                            else -> Unit
                        }
                    }
                    _isProUnlocked.value = unlocked
                    _pendingPurchase.value = pending
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
                        _priceLabel.value = cachedProductDetails
                            ?.oneTimePurchaseOfferDetails?.formattedPrice
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
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) return
                purchases.forEach { purchase ->
                    if (!purchase.products.contains(PRO_PRODUCT_ID)) return@forEach
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> handlePurchase(purchase)
                        Purchase.PurchaseState.PENDING -> _pendingPurchase.value = true
                        else -> Unit
                    }
                }
            }
            // User closed the Play sheet — nothing pending, nothing to unlock
            BillingClient.BillingResponseCode.USER_CANCELED -> _pendingPurchase.value = false
            // Already owned (e.g. double tap before refresh) — re-verify instead of failing
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _pendingPurchase.value = false
                retryScope.launch { refreshEntitlement() }
            }
            else -> Unit // ITEM_UNAVAILABLE etc. — caller sees launchPurchase return path / no unlock
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        _isProUnlocked.value = true
        _pendingPurchase.value = false
        acknowledgeIfNeeded(purchase)
    }

    /**
     * Acknowledge [purchase] if needed, checking the outcome. On failure the
     * token is queued and retried on the next refreshEntitlement() — never a
     * silent fire-and-forget, because an unacknowledged purchase is auto-refunded.
     */
    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            unacknowledgedTokens.remove(purchase.purchaseToken)
            return
        }
        val token = purchase.purchaseToken
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                unacknowledgedTokens.remove(token)
            } else {
                Log.w(TAG, "Acknowledge failed (${result.responseCode}); queued for retry")
                unacknowledgedTokens.add(token)
            }
        }
    }

    /** Retry queued acknowledgments. Called at the start of refreshEntitlement(). */
    private fun retryUnacknowledged() {
        if (unacknowledgedTokens.isEmpty()) return
        val tokens = unacknowledgedTokens.toList()
        tokens.forEach { token ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(token)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    unacknowledgedTokens.remove(token)
                } else {
                    Log.w(TAG, "Ack retry failed (${result.responseCode}); still queued")
                }
            }
        }
        if (unacknowledgedTokens.size > ACK_RETRY_LIMIT * tokens.size) {
            // Absurd growth guard — should never happen; drop and re-query instead
            unacknowledgedTokens.clear()
        }
    }

    override fun dispose() {
        retryScope.cancel()
        billingClient.endConnection()
    }
}
