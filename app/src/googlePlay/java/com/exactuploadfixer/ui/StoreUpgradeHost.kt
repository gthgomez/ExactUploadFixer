package com.exactuploadfixer.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Google Play placeholder while Amazon is the active launch target.
 *
 * The previous shared UI accidentally opened RevenueCat's Amazon-oriented
 * paywall in Google builds. Keep Google compiling without pretending that path
 * is launch-ready.
 */
@Composable
internal fun StoreUpgradeHost(
    showPaywall: Boolean,
    showCustomerCenter: Boolean,
    onDismissPaywall: () -> Unit,
    onDismissCustomerCenter: () -> Unit,
    onEntitlementChanged: () -> Unit,
    onRequestPurchase: () -> Unit
) {
    if (showPaywall) {
        AlertDialog(
            onDismissRequest = onDismissPaywall,
            title = { Text("Unlock Premium Presets") },
            text = { Text("Upgrade to Pro to unlock precise, verified size limits and exact dimensions for Passport, Visa, LinkedIn, and more portal uploads.") },
            confirmButton = {
                TextButton(onClick = {
                    onRequestPurchase()
                    onDismissPaywall()
                }) {
                    Text("Buy Lifetime Access")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPaywall) {
                    Text("Maybe later")
                }
            }
        )
    }

    if (showCustomerCenter) {
        AlertDialog(
            onDismissRequest = onDismissCustomerCenter,
            title = { Text("Manage purchase") },
            text = { Text("Manage this lifetime purchase through your Google Play account order history.") },
            confirmButton = {
                TextButton(onClick = {
                    onEntitlementChanged()
                    onDismissCustomerCenter()
                }) {
                    Text("OK")
                }
            }
        )
    }
}
