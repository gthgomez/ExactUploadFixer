package com.exactuploadfixer.domain

/**
 * Input contract for the engine. Both or neither dimension must be set.
 * One-sided input is ambiguous and will be rejected by validate().
 *
 * Byte-limit policy: [maxBytes] is always an exact byte count. Free-typed
 * "KB" values are interpreted DECIMALLY (1 KB = 1,000 bytes) because external
 * upload forms write "240 kB" without defining it, and the decimal reading is
 * the conservative one — an output under 240,000 bytes also satisfies a
 * 240 KiB (245,760-byte) check, but not vice versa. Presets skip KB entirely
 * and carry their own exact, source-verified byte caps via [fromPreset].
 */
data class FixConstraints(
    val maxBytes: Long,
    val targetWidth: Int? = null,
    val targetHeight: Int? = null
) {
    val hasDimensions: Boolean get() = targetWidth != null && targetHeight != null

    companion object {
        const val BYTES_PER_KB = 1000L

        fun fromPreset(preset: Preset): FixConstraints = FixConstraints(
            maxBytes = preset.maxBytes,
            targetWidth = preset.width,
            targetHeight = preset.height
        )

        /** Decimal KB → exact bytes (1 KB = 1,000 bytes). See class doc. */
        fun fromKb(maxSizeKb: Long, width: Int? = null, height: Int? = null): FixConstraints =
            FixConstraints(maxBytes = maxSizeKb * BYTES_PER_KB, targetWidth = width, targetHeight = height)
    }
}
