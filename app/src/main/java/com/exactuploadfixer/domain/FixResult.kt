package com.exactuploadfixer.domain

/**
 * The output of one successful compression pass.
 * bytes is the actual JPEG data — never empty on Success.
 * fileSizeBytes is always <= the requested maxBytes on Success and Degraded.
 */
data class ProcessedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val qualityUsed: Int,
    val fileSizeBytes: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedImage) return false
        return width == other.width &&
            height == other.height &&
            qualityUsed == other.qualityUsed &&
            fileSizeBytes == other.fileSizeBytes &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + qualityUsed
        result = 31 * result + fileSizeBytes.hashCode()
        return result
    }
}

/**
 * All explicit failure states. No silent fallbacks — every failure
 * produces a user-visible message in ResultScreen.
 * Source: ChatGPT Step 2 "Failure states that must be explicit".
 */
sealed interface FixFailure {
    data object InvalidInput : FixFailure
    data object UnsupportedMimeType : FixFailure      // not JPEG
    data object DecodeFailed : FixFailure              // corrupt / unreadable
    data object MemoryBudgetExceeded : FixFailure      // OOM risk after subsampling
    data object InvalidTargetDimensions : FixFailure   // one-sided or negative
    data object CompressionCouldNotMeetMaxSize : FixFailure  // quality floor reached
    data object ProcessingTimedOut : FixFailure        // guardrail tripped
    data object SaveFailed : FixFailure
}

/**
 * Explicit reasons why an output is usable but degraded versus the ideal path.
 * Degraded still guarantees fileSizeBytes <= requested maxBytes.
 */
sealed interface FixDegradation {
    data class MemoryConstrained(
        val sampleSize: Int,
        val outputWidth: Int,
        val outputHeight: Int
    ) : FixDegradation

    data class DimensionReduced(
        val scaleFactor: Float,
        val pass: Int
    ) : FixDegradation
}

/**
 * Top-level result. Either a success with the processed image
 * or an explicit failure with a typed reason.
 * Guarantee:
 *   - Success.image.fileSizeBytes <= requested maxBytes
 *   - Degraded.image.fileSizeBytes <= requested maxBytes
 *   - Failure is explicit; there is no silent over-limit export
 */
sealed interface FixResult {
    data class Processing(
        val quality: Int,
        val pass: Int = 1,
        val scaleFactor: Float = 1f
    ) : FixResult

    data class Fallback(
        val scaleFactor: Float,
        val pass: Int = 1
    ) : FixResult

    data class Success(val image: ProcessedImage) : FixResult
    data class Degraded(
        val image: ProcessedImage,
        val reasons: List<FixDegradation>
    ) : FixResult
    data class Failure(val reason: FixFailure) : FixResult

    val isSuccess: Boolean get() = this is Success || this is Degraded
}
