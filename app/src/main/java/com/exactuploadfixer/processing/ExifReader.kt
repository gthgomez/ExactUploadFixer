package com.exactuploadfixer.processing

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * The EXIF orientation correction needed to display a stored image upright.
 *
 * [mirrorHorizontal] must be applied BEFORE [rotationDegrees] (a clockwise
 * rotation). Pure rotations are correct for orientations 3/6/8; the mirrored
 * orientations (2/4/5/7) require a horizontal flip composed with the rotation —
 * treating them as a rotation-only would produce a mirrored image.
 */
data class ExifTransform(
    val mirrorHorizontal: Boolean,
    val rotationDegrees: Float
)

/**
 * Reads EXIF metadata from a URI safely.
 *
 * Risk addressed: ChatGPT Step 1, Risk #4 — EXIF orientation must be normalized
 * BEFORE crop math, otherwise center-crop targets the wrong region.
 */
object ExifReader {

    /**
     * Returns the full transform (mirror + clockwise rotation) needed to make the
     * image display upright. Safe default is the identity transform if EXIF is
     * unreadable or malformed.
     */
    fun readTransform(context: Context, uri: Uri): ExifTransform {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    // Mirror-only and rotation-only cases.
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifTransform(true, 0f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> ExifTransform(false, 180f)
                    ExifInterface.ORIENTATION_ROTATE_90 -> ExifTransform(false, 90f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> ExifTransform(false, 270f)

                    // Mirrored + rotated cases (transpose / transverse).
                    // Pixel-verified against Skia (BitmapTransformsExifTest): the
                    // horizontal flip composes with a 270° rotation for TRANSPOSE and
                    // a 90° rotation for TRANSVERSE.
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifTransform(true, 180f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> ExifTransform(true, 270f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> ExifTransform(true, 90f)

                    else -> ExifTransform(false, 0f)
                }
            } ?: ExifTransform(false, 0f)
        } catch (e: Exception) {
            ExifTransform(false, 0f)
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
