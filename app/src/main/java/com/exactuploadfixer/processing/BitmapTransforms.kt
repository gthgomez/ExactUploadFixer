package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Pure bitmap transforms — rotate, center-crop, sample-size calculation.
 * All functions recycle intermediate bitmaps they create.
 */
object BitmapTransforms {

    /**
     * Rotates [source] clockwise by [degrees]. Returns source unchanged if degrees == 0f.
     * Recycles source if a new bitmap is created.
     */
    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()
        return rotated
    }

    /**
     * Scale-to-fill then center-crop to exact [targetWidth] × [targetHeight].
     *
     * V1 constraint: center-crop only — no freeform crop UI.
     * This is intentional; freeform crop adds complexity without fixing the upload blocker.
     */
    fun centerCropTo(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) return source

        val sourceRatio = source.width.toFloat() / source.height
        val targetRatio = targetWidth.toFloat() / targetHeight

        val (scaledW, scaledH) = if (sourceRatio > targetRatio) {
            // Wider than target — scale by height, then crop width
            val h = targetHeight
            val w = (h * sourceRatio).toInt()
            w to h
        } else {
            // Taller than target — scale by width, then crop height
            val w = targetWidth
            val h = (w / sourceRatio).toInt()
            w to h
        }

        val scaled = if (source.width == scaledW && source.height == scaledH) {
            source
        } else {
            Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        }
        val x = (scaledW - targetWidth) / 2
        val y = (scaledH - targetHeight) / 2
        val cropped = if (x == 0 && y == 0 && scaled.width == targetWidth && scaled.height == targetHeight) {
            scaled
        } else {
            Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
        }

        if (scaled !== source && scaled !== cropped) scaled.recycle()
        if (source !== scaled && source !== cropped) source.recycle()
        return cropped
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
