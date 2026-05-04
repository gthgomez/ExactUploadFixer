package com.exactuploadfixer.processing

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * Reads EXIF metadata from a URI safely.
 *
 * Risk addressed: ChatGPT Step 1, Risk #4 — EXIF orientation must be normalized
 * BEFORE crop math, otherwise center-crop targets the wrong region.
 */
object ExifReader {

    /**
     * Returns the clockwise degrees needed to make the image display upright.
     * Safe default is 0f (no rotation) if EXIF is unreadable.
     */
    fun readRotationDegrees(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_TRANSPOSE -> 90f

                    ExifInterface.ORIENTATION_ROTATE_180,
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f

                    ExifInterface.ORIENTATION_ROTATE_270,
                    ExifInterface.ORIENTATION_TRANSVERSE -> 270f

                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Validates the URI is actually a JPEG using both MIME type and magic-byte check.
     *
     * Risk addressed: ChatGPT Step 1, Risk #2 — Photo Picker allows any image type.
     * MIME type alone is unreliable (can be wrong). Magic-byte check is the ground truth.
     */
    fun isJpeg(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)
        if (mime == "image/jpeg") return true

        // Fallback: check JPEG SOI marker (FF D8 FF)
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(3)
                val read = stream.read(header)
                read == 3 &&
                    header[0] == 0xFF.toByte() &&
                    header[1] == 0xD8.toByte() &&
                    header[2] == 0xFF.toByte()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
