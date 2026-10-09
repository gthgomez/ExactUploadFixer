package com.exactuploadfixer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle

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
    onRequestPurchase: () -> Unit,
    priceLabel: String? = null,
    pendingPurchase: Boolean = false
) {
    if (showPaywall) {
        AlertDialog(
            onDismissRequest = onDismissPaywall,
            title = { Text("Unlock Premium Presets") },
            text = {
                Column {
                    Text("Upgrade to Pro to unlock precise, verified size limits and exact dimensions for Passport, Visa, LinkedIn, and more portal uploads.")
                    // Only ever show the store-provided localized price — never an invented one
                    if (priceLabel != null) {
                        Text(
                            "Lifetime access: $priceLabel — one-time purchase via Google Play.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (pendingPurchase) {
                        Text(
                            "A previous purchase is still processing with Google Play. It will unlock automatically once the payment completes — you will not be charged twice.",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            },
            confirmButton = {
                // Never let the purchase flow start before the store has told us
                // the real localized price — launchBillingFlow would either fail
                // or present an amount the user has not seen.
                TextButton(
                    onClick = {
                        onRequestPurchase()
                        onDismissPaywall()
                    },
                    enabled = priceLabel != null
                ) {
                    Text(priceLabel?.let { "Buy Lifetime ($it)" } ?: "Loading price…")
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
