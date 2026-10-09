package com.exactuploadfixer.ui

import java.util.Locale

/**
 * Byte-count formatting for the UI.
 *
 * The app's byte policy (see FixConstraints) is DECIMAL: 1 KB = 1,000 bytes.
 * All user-facing size text must go through these helpers so the number a user
 * reads is always the same unit the input field and the engine use.
 */
private const val BYTES_PER_KB = 1000.0

/** "240" for 240,000 bytes, "307.2" for 307,200 bytes — decimal KB, no unit. */
fun formatKb(bytes: Long): String {
    val kb = bytes / BYTES_PER_KB
    val rounded = Math.round(kb * 10.0) / 10.0
    return if (rounded == Math.floor(rounded) && rounded < 1000.0) {
        String.format(Locale.US, "%.0f", rounded)
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

/** Integer decimal KB for filenames like fixed_240kb.jpg. */
fun formatKbInt(bytes: Long): Int = Math.round(bytes / BYTES_PER_KB).toInt()
