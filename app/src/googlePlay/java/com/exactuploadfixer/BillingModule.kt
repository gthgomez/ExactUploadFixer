package com.exactuploadfixer

import android.content.Context
import com.exactuploadfixer.billing.BillingGateway
import com.exactuploadfixer.billing.PlayBillingGateway

/**
 * Composition root for billing — googlePlay flavor.
 * Returns the Google Play Billing implementation.
 *
 * MainActivity calls BillingModule.create() without knowing which store is active.
 * The flavor source set determines which implementation is compiled in.
 */
object BillingModule {
    fun create(context: Context): BillingGateway = PlayBillingGateway(context)
}
