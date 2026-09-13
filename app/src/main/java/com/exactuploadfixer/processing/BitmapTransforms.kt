package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.roundToInt

/**
 * Pure bitmap transforms — rotate, center-crop, sample-size calculation.
 * All functions recycle intermediate bitmaps they create.
 */
object BitmapTransforms {

    /**
     * Returns true when an EXIF rotation of 90° or 270° (including the mirrored
     * transpose/transverse cases) swaps the effective width and height.
     * Must be applied to the decoded bounds BEFORE crop/resize math.
     */
    fun orientationSwapsDimensions(rotationDegrees: Float): Boolean =
        rotationDegrees == 90f || rotationDegrees == 270f

    /**
     * Applies the full EXIF orientation correction (mirror + clockwise rotation).
     * Returns source unchanged when the transform is the identity.
     * Recycles source if a new bitmap is created.
     */
    fun applyOrientation(source: Bitmap, transform: ExifTransform): Bitmap {
        if (!transform.mirrorHorizontal && transform.rotationDegrees == 0f) return source

        val matrix = Matrix()
        // Order matters: the horizontal flip must be composed with the rotation
        // so mirrored orientations (2/4/5/7) produce a correct upright image
        // rather than a mirrored one. Verified by BitmapTransformsExifTest.
        if (transform.mirrorHorizontal) matrix.postScale(-1f, 1f)
        if (transform.rotationDegrees != 0f) matrix.postRotate(transform.rotationDegrees)

        val transformed =
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (transformed !== source) source.recycle()
        return transformed
    }

    /**
     * Scale-to-fill then center-crop to exact [targetWidth] × [targetHeight].
     *
     * Memory-safe: instead of scaling the whole source up to fill and then cropping
     * (which can allocate an enormous intermediate for extreme aspect ratios — e.g.
     * a 40,000×40 source to a 400×400 target would build a 16-million-pixel-wide
     * scaled bitmap and OOM), it crops the source region that maps to the target
     * under the fill scale, then scales that region to the exact target size.
     * Intermediates stay bounded by the source and target dimensions.
     *
     * V1 constraint: center-crop only — no freeform crop UI.
     */
    fun centerCropTo(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) return source

        val sw = source.width
        val sh = source.height

        // Scale-to-fill factor: how much the source must be scaled so one dimension
        // exactly covers the target and the other overflows (then center-cropped).
        val fillScale = maxOf(
            targetWidth.toFloat() / sw,
            targetHeight.toFloat() / sh
        )

        // The source region (centered) that maps onto the target under fillScale.
        val cropW = (targetWidth / fillScale).coerceIn(1f, sw.toFloat())
        val cropH = (targetHeight / fillScale).coerceIn(1f, sh.toFloat())
        val cropWInt = cropW.roundToInt().coerceAtMost(sw)
        val cropHInt = cropH.roundToInt().coerceAtMost(sh)
        val x = (sw - cropWInt) / 2
        val y = (sh - cropHInt) / 2

        val crop = Bitmap.createBitmap(source, x, y, cropWInt, cropHInt)
        if (crop !== source) source.recycle()

        val result = if (crop.width == targetWidth && crop.height == targetHeight) {
            crop
        } else {
            val scaled = Bitmap.createScaledBitmap(crop, targetWidth, targetHeight, true)
            if (scaled !== crop) crop.recycle()
            scaled
        }
        return result
    }

    /**
     * Largest power-of-2 sample size that keeps decoded bitmap ≥ target dimensions.
     *
     * Risk addressed: ChatGPT Step 1, Risk #3 — large bitmaps trigger OOM.
     * Decoding at full resolution then scaling is the most common crash cause.
     */
    fun calculateSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1
        var halfW = sourceWidth / 2
        var halfH = sourceHeight / 2
        while (halfW >= targetWidth && halfH >= targetHeight) {
            sampleSize *= 2
            halfW /= 2
            halfH /= 2
        }
        return sampleSize
    }
}
