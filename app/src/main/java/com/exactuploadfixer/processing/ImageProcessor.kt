package com.exactuploadfixer.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixDegradation
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.UploadFixerEngine
import kotlin.math.floor
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Production implementation of UploadFixerEngine.
 *
 * Guarantees:
 *   1. Success and Degraded always satisfy image.fileSizeBytes <= maxBytes
 *   2. Fixed-dimension requests never silently shrink below the requested size
 *   3. Any non-success terminal condition is explicit (Failure or Degraded)
 *   4. Processing is bounded by timeout + iteration ceilings
 */
class ImageProcessor(private val context: Context) : UploadFixerEngine {

    override suspend fun process(sourceUri: Uri, constraints: FixConstraints): Flow<FixResult> =
        flow {
            val completed = withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
                runPipeline(sourceUri, constraints)
                true
            }
            if (completed == null) {
                emit(FixResult.Failure(FixFailure.ProcessingTimedOut))
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<FixResult>.runPipeline(
        sourceUri: Uri,
        constraints: FixConstraints
    ) {
        validate(constraints, sourceUri)?.let {
            emit(FixResult.Failure(it))
            return
        }

        val sourceInfo = readSourceInfo(sourceUri)
        if (sourceInfo == null) {
            emit(FixResult.Failure(FixFailure.DecodeFailed))
            return
        }

        val targetWidth = constraints.targetWidth ?: sourceInfo.effectiveWidth
        val targetHeight = constraints.targetHeight ?: sourceInfo.effectiveHeight
        val sampleSize = calculateGuardedSampleSize(
            sourceWidth = sourceInfo.effectiveWidth,
            sourceHeight = sourceInfo.effectiveHeight,
            targetWidth = targetWidth,
            targetHeight = targetHeight
        )

        var rawBitmap: Bitmap? = null
        var workingBitmap: Bitmap? = null
        var passBitmap: Bitmap? = null

        try {
            rawBitmap = decodeBitmap(sourceUri, sampleSize)
                ?: run {
                    emit(FixResult.Failure(FixFailure.DecodeFailed))
                    return
                }

            val upright = BitmapTransforms.applyOrientation(rawBitmap, sourceInfo.transform)
            rawBitmap = null

            val working = if (constraints.hasDimensions) {
                BitmapTransforms.centerCropTo(
                    upright,
                    constraints.targetWidth!!,
                    constraints.targetHeight!!
                )
            } else {
                upright
            }
            workingBitmap = working

            val degradationReasons = mutableListOf<FixDegradation>()
            if (!constraints.hasDimensions && sampleSize > 1) {
                degradationReasons += FixDegradation.MemoryConstrained(
                    sampleSize = sampleSize,
                    outputWidth = working.width,
                    outputHeight = working.height
                )
            }

            passBitmap = working
            var cumulativeScaleFactor = 1f
            var totalIterations = 0

            for (pass in 1..MAX_FALLBACK_PASSES) {
                val remainingIterations = MAX_TOTAL_SEARCH_ITERATIONS - totalIterations
                if (remainingIterations <= 0) break
                val currentBitmap = passBitmap ?: break

                if (pass > 1) {
                    emit(FixResult.Fallback(scaleFactor = cumulativeScaleFactor, pass = pass))
                }

                val searchResult = JpegCompression.searchUnderMaxSize(
                    bitmap = currentBitmap,
                    maxBytes = constraints.maxBytes,
                    maxIterations = minOf(
                        remainingIterations,
                        JpegCompression.DEFAULT_MAX_ITERATIONS_PER_PASS
                    ),
                    onQualityTested = { quality ->
                        emit(
                            FixResult.Processing(
                                quality = quality,
                                pass = pass,
                                scaleFactor = cumulativeScaleFactor
                            )
                        )
                    }
                )
                totalIterations += searchResult.iterations

                when (searchResult) {
                    is JpegCompression.SearchResult.Fit -> {
                        if (degradationReasons.isEmpty()) {
                            emit(FixResult.Success(searchResult.image))
                        } else {
                            emit(
                                FixResult.Degraded(
                                    image = searchResult.image,
                                    reasons = degradationReasons.toList()
                                )
                            )
                        }
                        return
                    }

                    is JpegCompression.SearchResult.EncodeFailure -> {
                        emit(FixResult.Failure(FixFailure.CompressionCouldNotMeetMaxSize))
                        return
                    }

                    is JpegCompression.SearchResult.NoFit -> {
                        if (constraints.hasDimensions) {
                            emit(FixResult.Failure(FixFailure.CompressionCouldNotMeetMaxSize))
                            return
                        }

                        val nextBitmap = createNextFallbackBitmap(
                            current = currentBitmap,
                            smallestBytesSeen = searchResult.smallestBytesSeen,
                            maxBytes = constraints.maxBytes
                        )
                        if (nextBitmap == null) {
                            emit(FixResult.Failure(FixFailure.CompressionCouldNotMeetMaxSize))
                            return
                        }

                        val previousWidth = currentBitmap.width
                        val scaledWidthRatio = nextBitmap.width.toFloat() / previousWidth.toFloat()
                        cumulativeScaleFactor *= scaledWidthRatio
                        degradationReasons += FixDegradation.DimensionReduced(
                            scaleFactor = cumulativeScaleFactor,
                            pass = pass + 1
                        )

                        if (nextBitmap !== currentBitmap) {
                            if (currentBitmap !== workingBitmap) recycle(currentBitmap)
                            passBitmap = nextBitmap
                        }
                    }
                }
            }

            emit(FixResult.Failure(FixFailure.CompressionCouldNotMeetMaxSize))
        } catch (_: OutOfMemoryError) {
            emit(FixResult.Failure(FixFailure.MemoryBudgetExceeded))
        } finally {
            recycle(rawBitmap)
            if (passBitmap !== workingBitmap) recycle(passBitmap)
            recycle(workingBitmap)
        }
    }

    private fun readSourceInfo(uri: Uri): SourceInfo? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        val transform = ExifReader.readTransform(context, uri)
        val (effectiveWidth, effectiveHeight) =
            if (BitmapTransforms.orientationSwapsDimensions(transform.rotationDegrees)) {
                rawHeight to rawWidth
            } else {
                rawWidth to rawHeight
            }

        return SourceInfo(
            effectiveWidth = effectiveWidth,
            effectiveHeight = effectiveHeight,
            transform = transform
        )
    }

    private fun decodeBitmap(uri: Uri, sampleSize: Int): Bitmap? {
        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateGuardedSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = BitmapTransforms.calculateSampleSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            targetWidth = targetWidth,
            targetHeight = targetHeight
        ).coerceAtLeast(1)

        while (
            estimatedPixelCount(sourceWidth, sourceHeight, sampleSize) > MAX_WORKING_PIXELS ||
            estimatedBitmapBytes(sourceWidth, sourceHeight, sampleSize) > MAX_WORKING_BITMAP_BYTES
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun createNextFallbackBitmap(
        current: Bitmap,
        smallestBytesSeen: Long,
        maxBytes: Long
    ): Bitmap? {
        if (current.width <= MIN_FALLBACK_EDGE || current.height <= MIN_FALLBACK_EDGE) return null

        val ratio = if (smallestBytesSeen > 0L && maxBytes > 0L) {
            maxBytes.toDouble() / smallestBytesSeen.toDouble()
        } else {
            0.0
        }
        val areaScale = sqrt(ratio.coerceAtLeast(0.0))
        val guardedScale = when {
            ratio <= 0.0 -> 0.7
            ratio < 0.15 -> minOf(areaScale * 0.82, 0.75)
            ratio < 0.40 -> minOf(areaScale * 0.88, 0.82)
            else -> minOf(areaScale * 0.94, 0.9)
        }.toFloat().coerceIn(MIN_SCALE_STEP, MAX_SCALE_STEP)

        val nextWidth = floor(current.width * guardedScale).toInt().coerceAtLeast(MIN_FALLBACK_EDGE)
        val nextHeight = floor(current.height * guardedScale).toInt().coerceAtLeast(MIN_FALLBACK_EDGE)
        if (nextWidth >= current.width || nextHeight >= current.height) return null

        return Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
    }

    private fun estimatedPixelCount(width: Int, height: Int, sampleSize: Int): Long {
        val scaledWidth = ((width + sampleSize - 1) / sampleSize).toLong()
        val scaledHeight = ((height + sampleSize - 1) / sampleSize).toLong()
        return scaledWidth * scaledHeight
    }

    private fun estimatedBitmapBytes(width: Int, height: Int, sampleSize: Int): Long =
        estimatedPixelCount(width, height, sampleSize) * BYTES_PER_PIXEL_RGB_565

    private fun recycle(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private fun validate(constraints: FixConstraints, uri: Uri): FixFailure? {
        if (constraints.maxBytes <= 0L) return FixFailure.InvalidInput

        val widthMissing = constraints.targetWidth == null
        val heightMissing = constraints.targetHeight == null
        if (widthMissing != heightMissing) return FixFailure.InvalidTargetDimensions
        if ((constraints.targetWidth ?: 1) <= 0 || (constraints.targetHeight ?: 1) <= 0) {
            return FixFailure.InvalidTargetDimensions
        }

        if (!ExifReader.isJpeg(context, uri)) return FixFailure.UnsupportedMimeType

        return null
    }

    private data class SourceInfo(
        val effectiveWidth: Int,
        val effectiveHeight: Int,
        val transform: ExifTransform
    )

    private companion object {
        private const val PROCESS_TIMEOUT_MS = 15_000L
        private const val MAX_FALLBACK_PASSES = 6
        private const val MAX_TOTAL_SEARCH_ITERATIONS = 48
        private const val MAX_WORKING_PIXELS = 16_000_000L
        private const val MAX_WORKING_BITMAP_BYTES = 48L * 1024L * 1024L
        private const val BYTES_PER_PIXEL_RGB_565 = 2L
        private const val MIN_FALLBACK_EDGE = 64
        private const val MIN_SCALE_STEP = 0.55f
        private const val MAX_SCALE_STEP = 0.9f
    }
}
