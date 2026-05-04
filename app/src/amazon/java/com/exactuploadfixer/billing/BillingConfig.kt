package com.exactuploadfixer.billing

/**
 * RevenueCat configuration constants for the Amazon Appstore build.
 *
 * ── REQUIRED SETUP BEFORE FIRST RELEASE ──────────────────────────────────────
 *
 * Step 1 — RevenueCat Dashboard (app.revenuecat.com):
 *   a. Create a project (or use existing).
 *   b. Add App → select "Amazon Appstore" → copy the "Public API key".
 *   c. Replace REVENUECAT_API_KEY below with that value.
 *   d. Entitlements → Add Entitlement → identifier = "pro_access" (must match ENTITLEMENT_PRO).
 *   e. Products → Add Product → identifier = "exact_upload_fixer_lifetime".
 *   f. Attach each product to the "pro_access" entitlement.
 *
 * Step 2 — Amazon Developer Console (developer.amazon.com):
 *   a. Your app listing → In-App Items → Add a Consumable/Entitlement.
 *   b. Set the same SKU in Amazon Developer Console: "exact_upload_fixer_lifetime".
 *   c. Set the price for the SKU as a one-time lifetime unlock.
 *   d. Submit and wait for Amazon IAP approval before testing live purchases.
 *
 * Step 3 — Before release, replace the development API key below with the
 *           production Amazon RevenueCat key.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Note on API key security: the RevenueCat public API key is NOT a secret.
 * It is safe to ship in the APK. It only allows reading public storefront data
 * and is scoped to your RevenueCat project — it cannot modify purchases.
 */
object BillingConfig {

    /**
     * RevenueCat public API key for the Amazon Appstore app.
     *
     * Where to find it:
     *   RevenueCat Dashboard → Your Project → Apps → Amazon Appstore → Public API key
     *
     * Production Amazon RevenueCat public API key.
     */
    const val REVENUECAT_API_KEY = "amzn_iVuGZDgQzFFxpvBRXJxtmqNRaQg"

    /**
     * Entitlement identifier that grants Pro access.
     *
     * Must match exactly what you created in:
     *   RevenueCat Dashboard → Entitlements → identifier field
     *
     * Default value "pro_access" — only change if your dashboard uses a different ID.
     */
    const val ENTITLEMENT_PRO = "pro_access"

    /**
     * Amazon IAP product SKU that unlocks Pro access.
     *
     * Must match exactly what you created in:
     *   Amazon Developer Console → Your App → In-App Items → SKU field
     *
     * Case-sensitive. RevenueCat uses these identifiers to map store products to Offerings.
     */
    const val AMAZON_LIFETIME_PRODUCT_ID = "exact_upload_fixer_lifetime"

    val AMAZON_PRODUCT_IDS = setOf(AMAZON_LIFETIME_PRODUCT_ID)
}
