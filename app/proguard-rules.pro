# ─── Google Play Billing (googlePlay flavor) ──────────────────────────────────
# Required: Play Billing does not ship consumer ProGuard rules.
# Missing = silent billing failure in release builds (isMinifyEnabled = true).
# Source: skill_google_play_billing Step 4, skill_android_release_build Step 3
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.vending.billing.** { *; }

# ─── RevenueCat Purchases SDK (amazon flavor) ─────────────────────────────────
# Required: RC does not ship consumer ProGuard rules that cover all internal classes.
-keep class com.revenuecat.purchases.** { *; }
# Amazon IAP SDK — transitive dependency of purchases-amazon
-keep class com.amazon.device.iap.** { *; }

# ─── Kotlin Coroutines ────────────────────────────────────────────────────────
# Required for correct suspend function behavior in minified release builds.
# Source: skill_android_release_build Step 3
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Parcelable ───────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
