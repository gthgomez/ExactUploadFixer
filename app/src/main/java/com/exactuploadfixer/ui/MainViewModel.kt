package com.exactuploadfixer.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exactuploadfixer.billing.BillingGateway
import com.exactuploadfixer.R
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.Preset
import com.exactuploadfixer.domain.UploadFixerEngine
import kotlinx.coroutines.launch

/**
 * Single ViewModel for the whole app (3-screen linear flow).
 *
 * Isolation rules (ChatGPT Step 6):
 *   - Never imports anything from processing.*
 *   - Knows UploadFixerEngine (domain interface), not ImageProcessor (impl)
 *   - Knows BillingGateway (interface), not PlayBillingGateway (impl)
 *   - UI reads uiState; never calls engine or billing directly
 *
 * Billing failure handling:
 *   If billing.connect() throws (offline, Play unavailable), free tier still works.
 *   isProUnlocked stays false — presets are locked, manual entry is not.
 */
class MainViewModel(
    application: Application,
    private val engine: UploadFixerEngine,
    private val billing: BillingGateway
) : AndroidViewModel(application) {

    private val contentResolver = application.contentResolver
    private val prefs = application.getSharedPreferences("exact_upload_fixer_prefs", Context.MODE_PRIVATE)

    /** In-flight processing job — cancelled when the flow is left or reset. */
    private var processJob: kotlinx.coroutines.Job? = null

    var uiState by mutableStateOf(
        AppUiState(
            screen = if (application.getSharedPreferences("exact_upload_fixer_prefs", Context.MODE_PRIVATE).getBoolean("onboarding_completed", false)) {
                AppScreen.Pick
            } else {
                AppScreen.Onboarding
            }
        )
    )
        private set

    fun onOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        uiState = uiState.copy(screen = AppScreen.Pick)
    }

    private companion object {
        // Matches the engine's bounded search budget so UI progress advances steadily.
        const val PROCESSING_PROGRESS_STEPS = 48f
    }

    init {
        // Observe billing state and push into UI state
        viewModelScope.launch {
            billing.isProUnlocked.collect { unlocked ->
                uiState = uiState.copy(isProUnlocked = unlocked)
            }
        }
        viewModelScope.launch {
            billing.pendingPurchase.collect { pending ->
                uiState = uiState.copy(pendingPurchase = pending)
            }
        }
        viewModelScope.launch {
            billing.priceLabel.collect { label ->
                uiState = uiState.copy(priceLabel = label)
            }
        }
        // Connect and check entitlement — failures are silent (free tier still works)
        viewModelScope.launch {
            try {
                billing.connect()
                billing.refreshEntitlement()
            } catch (e: Exception) {
                // Billing unavailable — isProUnlocked remains false
            }
        }
    }

    // ── Pick screen ──────────────────────────────────────────────────────────

    fun onPhotoPicked(uri: Uri?) {
        uri ?: return

        // A new pick invalidates any in-flight run for the previous photo
        cancelProcessing()

        // Reject non-JPEG before navigating away from PickScreen.
        // Mirrors ExifReader.isJpeg without importing from processing.*:
        // check MIME type first, fall back to SOI magic bytes (FF D8 FF).
        val isJpeg = contentResolver.getType(uri) == "image/jpeg" || try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(3)
                val read = stream.read(header)
                read == 3 &&
                    header[0] == 0xFF.toByte() &&
                    header[1] == 0xD8.toByte() &&
                    header[2] == 0xFF.toByte()
            } ?: false
        } catch (_: Exception) { false }

        if (!isJpeg) {
            uiState = uiState.copy(pickError = getApplication<Application>().getString(R.string.pick_error_jpeg_only))
            return
        }

        // Read source file size for display in Edit/Result screens
        val sizeBytes: Long? = try {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize.takeIf { s -> s > 0 } }
        } catch (_: Exception) { null }

        uiState = uiState.copy(
            selectedUri = uri,
            sourceSizeBytes = sizeBytes,
            screen = AppScreen.Edit,
            pickError = null,
            editError = null,
            result = null,
            resultFailure = null,
            maxSizeKbInput = "",
            widthInput = "",
            heightInput = "",
            selectedPreset = null
        )
    }

    // ── Edit screen ──────────────────────────────────────────────────────────

    fun onMaxSizeChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(4)
        uiState = uiState.copy(maxSizeKbInput = filtered, selectedPreset = null, editError = null)
    }

    fun onWidthChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(4)
        uiState = uiState.copy(widthInput = filtered, selectedPreset = null, editError = null)
    }

    fun onHeightChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(4)
        uiState = uiState.copy(heightInput = filtered, selectedPreset = null, editError = null)
    }

    fun onPresetSelected(preset: Preset) {
        // Fill the size field with a conservative DECIMAL KB value derived from the
        // preset's exact byte cap (rounded up), so re-processing through the input
        // path never exceeds the preset limit. Processing itself uses preset.maxBytes
        // directly (see onProcessClick) — the field is display/fallback only.
        val conservativeKb = (preset.maxBytes + FixConstraints.BYTES_PER_KB - 1) / FixConstraints.BYTES_PER_KB
        uiState = uiState.copy(
            selectedPreset = preset,
            widthInput = preset.width.toString(),
            heightInput = preset.height.toString(),
            maxSizeKbInput = conservativeKb.toString(),
            editError = null
        )
    }

    fun onProcessClick() {
        if (uiState.isProcessing) return
        val uri = uiState.selectedUri ?: return

        // Presets carry an exact, source-verified byte cap — use it directly so the
        // KB field never round-trips the limit lossily (e.g. DV 240,000 bytes).
        // Free-typed values are decimal KB: 1 KB = 1,000 bytes (see FixConstraints).
        val selectedPreset = uiState.selectedPreset
        val maxBytes = selectedPreset?.maxBytes
            ?: uiState.maxSizeKbInput.toLongOrNull()?.let { it * FixConstraints.BYTES_PER_KB }
        if (maxBytes == null || maxBytes <= 0L) {
            uiState = uiState.copy(editError = "Enter a valid max file size in KB")
            return
        }

        val w = uiState.widthInput.toIntOrNull()
        val h = uiState.heightInput.toIntOrNull()
        if ((w == null) != (h == null)) {
            uiState = uiState.copy(editError = "Enter both width and height, or leave both empty")
            return
        }
        if ((w ?: 1) <= 0 || (h ?: 1) <= 0) {
            uiState = uiState.copy(editError = "Width and height must be positive")
            return
        }

        val constraints = FixConstraints(maxBytes = maxBytes, targetWidth = w, targetHeight = h)
        uiState = uiState.copy(
            isProcessing = true,
            editError = null,
            currentQuality = null,
            processingProgress = 0f,
            isFallback = false
        )

        // Cancel any stale in-flight run before starting a new one so engine
        // emissions from the old constraints can never land in the new state.
        processJob?.cancel()
        processJob = viewModelScope.launch {
            var testedProbeCount = 0f
            try {
                engine.process(uri, constraints).collect { result ->
                    uiState = when (result) {
                    is FixResult.Processing -> {
                        testedProbeCount += 1f
                        uiState.copy(
                            currentQuality = result.quality,
                            processingProgress = (testedProbeCount / PROCESSING_PROGRESS_STEPS).coerceAtMost(0.95f)
                        )
                    }
                    is FixResult.Fallback -> uiState.copy(
                        isFallback = true,
                        processingProgress = uiState.processingProgress ?: 0f
                    )
                    is FixResult.Success -> uiState.copy(
                        isProcessing = false,
                        isFallback = false,
                        currentQuality = null,
                        processingProgress = 1f,
                        result = result.image,
                        resultFailure = null,
                        screen = AppScreen.Result
                    )
                    is FixResult.Degraded -> uiState.copy(
                        isProcessing = false,
                        isFallback = true,
                        currentQuality = null,
                        processingProgress = 1f,
                        result = result.image,
                        resultFailure = null,
                        screen = AppScreen.Result
                    )
                    is FixResult.Failure -> uiState.copy(
                        isProcessing = false,
                        currentQuality = null,
                        processingProgress = 1f,
                        result = null,
                        resultFailure = result.reason,
                        screen = AppScreen.Result
                    )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Superseded or torn down — leave state cleanup to the caller
                throw e
            } finally {
                // If the job ended without a terminal emission (cancellation),
                // make sure the UI never sticks in isProcessing.
                if (uiState.isProcessing) {
                    uiState = uiState.copy(isProcessing = false, currentQuality = null)
                }
            }
        }
    }

    private fun cancelProcessing() {
        processJob?.cancel()
        processJob = null
        if (uiState.isProcessing) {
            uiState = uiState.copy(isProcessing = false, currentQuality = null, processingProgress = null)
        }
    }

    // ── Result screen ────────────────────────────────────────────────────────

    fun onBackToEdit() {
        cancelProcessing()
        uiState = uiState.copy(
            screen = AppScreen.Edit,
            result = null,
            resultFailure = null,
            isFallback = false,
            currentQuality = null,
            processingProgress = null
        )
    }

    fun onStartOver() {
        // Reset everything except billing state
        cancelProcessing()
        uiState = AppUiState(isProUnlocked = uiState.isProUnlocked)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Called from MainActivity.onResume(). Re-establishes billing connection if
     * it dropped while the app was backgrounded, then re-verifies entitlement.
     * Overlay contract: "queryPurchasesAsync on every onResume — never rely on cached state."
     */
    fun onResume() {
        refreshBillingEntitlement()
    }

    fun refreshBillingEntitlement() {
        viewModelScope.launch {
            try {
                billing.connect()
                billing.refreshEntitlement()
            } catch (e: Exception) {
                // Billing unavailable — free tier continues to work
            }
        }
    }

    // ── Billing ──────────────────────────────────────────────────────────────

    fun buyPro(activity: Activity): Boolean = billing.launchPurchase(activity)

    override fun onCleared() {
        super.onCleared()
        billing.dispose()
    }
}
