package com.exactuploadfixer.domain

/**
 * Input contract for the engine. Both or neither dimension must be set.
 * One-sided input is ambiguous and will be rejected by validate().
 */
data class FixConstraints(
    val maxBytes: Long,
    val targetWidth: Int? = null,
    val targetHeight: Int? = null
) {
    val hasDimensions: Boolean get() = targetWidth != null && targetHeight != null

    companion object {
        fun fromPreset(preset: Preset): FixConstraints = FixConstraints(
            maxBytes = preset.maxBytes,
            targetWidth = preset.width,
            targetHeight = preset.height
        )

        fun fromKb(maxSizeKb: Long, width: Int? = null, height: Int? = null): FixConstraints =
            FixConstraints(maxBytes = maxSizeKb * 1024L, targetWidth = width, targetHeight = height)
    }
}
