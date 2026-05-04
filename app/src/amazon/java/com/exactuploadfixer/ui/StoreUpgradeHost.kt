package com.exactuploadfixer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.exactuploadfixer.billing.BillingConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * Amazon flavor upgrade surface.
 *
 * RevenueCat owns the paywall and Amazon IAP transaction UI; the app still owns
 * entitlement state through BillingGateway, so every purchase/restore completion
 * explicitly asks MainViewModel to refresh pro access.
 */
@Composable
internal fun StoreUpgradeHost(
    showPaywall: Boolean,
    showCustomerCenter: Boolean,
    onDismissPaywall: () -> Unit,
    onDismissCustomerCenter: () -> Unit,
    onEntitlementChanged: () -> Unit
) {
    if (showPaywall) {
        PaywallDialog(
            paywallDialogOptions = PaywallDialogOptions.Builder()
                .setRequiredEntitlementIdentifier(BillingConfig.ENTITLEMENT_PRO)
                .setDismissRequest(onDismissPaywall)
                .setShouldDisplayDismissButton(true)
                .setListener(object : PaywallListener {
                    override fun onPurchaseCompleted(
                        customerInfo: CustomerInfo,
                        storeTransaction: StoreTransaction
                    ) {
                        onEntitlementChanged()
                        onDismissPaywall()
                    }

                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        onEntitlementChanged()
                        onDismissPaywall()
                    }

                    override fun onPurchaseError(error: PurchasesError) {
                        onEntitlementChanged()
                    }

                    override fun onPurchaseCancelled() {
                        onEntitlementChanged()
                    }

                    override fun onRestoreError(error: PurchasesError) {
                        onEntitlementChanged()
                    }

                    override fun onPurchaseStarted(rcPackage: Package) = Unit
                    override fun onRestoreStarted() = Unit
                })
                .build()
        )
    }

    if (showCustomerCenter) {
        CustomerCenter(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {
                onEntitlementChanged()
                onDismissCustomerCenter()
            }
        )
    }
}
