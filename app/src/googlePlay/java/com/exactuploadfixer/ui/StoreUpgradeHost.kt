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
    onEntitlementChanged: () -> Unit
) {
    if (showPaywall) {
        AlertDialog(
            onDismissRequest = onDismissPaywall,
            title = { Text("Upgrade unavailable") },
            text = { Text("Google Play upgrade is disabled in this build while Amazon launch testing is in progress.") },
            confirmButton = {
                TextButton(onClick = {
                    onEntitlementChanged()
                    onDismissPaywall()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCustomerCenter) {
        AlertDialog(
            onDismissRequest = onDismissCustomerCenter,
            title = { Text("Manage purchase unavailable") },
            text = { Text("Purchase management is not wired for this Google Play build yet.") },
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
