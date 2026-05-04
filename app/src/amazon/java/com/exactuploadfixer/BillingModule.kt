package com.exactuploadfixer

import android.content.Context
import com.exactuploadfixer.billing.BillingGateway
import com.exactuploadfixer.billing.RevenueCatBillingGateway

/**
 * Composition root for billing — amazon flavor.
 * Returns the RevenueCat/Amazon IAP implementation.
 *
 * MainActivity calls BillingModule.create() without knowing which store is active.
 * The flavor source set determines which implementation is compiled in.
 */
object BillingModule {
    fun create(context: Context): BillingGateway = RevenueCatBillingGateway(context)
}
