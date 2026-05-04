package com.exactuploadfixer.ui

import android.net.Uri
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.Preset
import com.exactuploadfixer.domain.ProcessedImage

/**
 * Three-screen navigation graph. No back-stack needed — flow is linear.
 * Source: ChatGPT Step 4.
 */
sealed interface AppScreen {
    data object Pick : AppScreen
    data object Edit : AppScreen
    data object Result : AppScreen
}

/**
 * Single source of truth for all UI state.
 *
 * KEY FIX vs all three reference chats:
 * ProcessedImage (which contains ByteArray) lives HERE, not in nav routes.
 * None of the three chats solved the ByteArray navigation problem:
 *   - Grok passed bytes.size (Int) — ResultScreen had no image to show
 *   - Gemini passed metadata strings — same problem
 *   - ChatGPT's ViewModel didn't wire up navigation at all
 *
 * Solution: ViewModel holds AppUiState in memory. Screens read from it via
 * collectAsState(). Navigation is just screen = AppScreen.Result.
 * The ByteArray never touches a nav route.
 */
data class AppUiState(
    val screen: AppScreen = AppScreen.Pick,

    // Set when user picks a photo
    val selectedUri: Uri? = null,
    val pickError: String? = null,

    /** Original file size in bytes — populated after pick for inline stats display. */
    val sourceSizeBytes: Long? = null,

    // Edit screen inputs
    val maxSizeKbInput: String = "",
    val widthInput: String = "",
    val heightInput: String = "",
    val selectedPreset: Preset? = null,
    val editError: String? = null,

    // Processing in flight
    val isProcessing: Boolean = false,
    val currentQuality: Int? = null,
    val processingProgress: Float? = null,
    val isFallback: Boolean = false,

    // Result screen — one of these will be non-null after processing
    val result: ProcessedImage? = null,
    val resultFailure: FixFailure? = null,

    // Billing — driven by BillingGateway.isProUnlocked StateFlow
    val isProUnlocked: Boolean = false
)
