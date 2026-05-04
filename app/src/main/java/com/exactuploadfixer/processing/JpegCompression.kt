package com.exactuploadfixer.processing

import android.graphics.Bitmap
import com.exactuploadfixer.domain.ProcessedImage
import java.io.ByteArrayOutputStream

/**
 * Bounded JPEG search. The processor owns the Flow emissions; this helper only
 * probes candidate qualities and returns a deterministic pass result.
 *
 * Strategy:
 *   1. Probe coarse anchor qualities to bracket a fit quickly
 *   2. Binary-search inside the bracket for a higher fitting quality
 *   3. Keep the best fitting candidate seen so far
 *
 * This does not assume a perfect monotonic size curve. If a pass finds no fit,
 * ImageProcessor downscales and retries in a later pass instead of silently
 * exporting an over-limit JPEG.
 */
object JpegCompression {

    const val QUALITY_MAX = 100
    const val QUALITY_FLOOR = 35
    const val DEFAULT_MAX_ITERATIONS_PER_PASS = 12

    private val QUALITY_ANCHORS = intArrayOf(100, 92, 84, 76, 68, 60, 52, 44, 35)

    sealed interface SearchResult {
        val iterations: Int

        data class Fit(
            val image: ProcessedImage,
            override val iterations: Int
        ) : SearchResult

        data class NoFit(
            val smallestBytesSeen: Long,
            override val iterations: Int
        ) : SearchResult

        data class EncodeFailure(
            override val iterations: Int
        ) : SearchResult
    }

    private data class Candidate(
        val quality: Int,
        val sizeBytes: Long,
        val bytes: ByteArray
    )

    private data class Probe(
        val quality: Int,
        val sizeBytes: Long,
        val bytes: ByteArray?
    )

    suspend fun searchUnderMaxSize(
        bitmap: Bitmap,
        maxBytes: Long,
        maxIterations: Int = DEFAULT_MAX_ITERATIONS_PER_PASS,
        onQualityTested: suspend (quality: Int) -> Unit
    ): SearchResult {
        require(maxBytes > 0L) { "maxBytes must be > 0" }
        require(maxIterations > 0) { "maxIterations must be > 0" }

        val stream = ByteArrayOutputStream()
        val testedSizes = mutableMapOf<Int, Long>()
        var iterations = 0
        var smallestBytesSeen = Long.MAX_VALUE
        var bestFit: Candidate? = null

        suspend fun testQuality(quality: Int): Probe? {
            if (quality !in QUALITY_FLOOR..QUALITY_MAX) return null

            val cachedSize = testedSizes[quality]
            if (cachedSize != null) {
                return Probe(quality = quality, sizeBytes = cachedSize, bytes = null)
            }
            if (iterations >= maxIterations) return null

            onQualityTested(quality)
            stream.reset()

            val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            iterations += 1
            if (!ok) return Probe(quality = quality, sizeBytes = -1L, bytes = null)

            val size = stream.size().toLong()
            testedSizes[quality] = size
            smallestBytesSeen = minOf(smallestBytesSeen, size)
            val bytes = if (size <= maxBytes) stream.toByteArray() else null
            return Probe(quality = quality, sizeBytes = size, bytes = bytes)
        }

        fun updateBestFit(probe: Probe) {
            val bytes = probe.bytes ?: return
            val current = bestFit
            if (current == null || probe.quality > current.quality) {
                bestFit = Candidate(
                    quality = probe.quality,
                    sizeBytes = probe.sizeBytes,
                    bytes = bytes
                )
            }
        }

        var highestOversizeQuality: Int? = null
        var lowestFitQuality: Int? = null

        for (quality in QUALITY_ANCHORS) {
            val probe = testQuality(quality) ?: break
            if (probe.sizeBytes < 0L) return SearchResult.EncodeFailure(iterations)

            if (probe.sizeBytes <= maxBytes) {
                updateBestFit(probe)
                lowestFitQuality = probe.quality
                if (probe.quality == QUALITY_MAX) {
                    return buildFitResult(bitmap, bestFit!!, iterations)
                }
                break
            } else {
                highestOversizeQuality = probe.quality
            }
        }

        val fitQuality = lowestFitQuality
        val oversizeQuality = highestOversizeQuality
        if (fitQuality != null && oversizeQuality != null) {
            var low: Int = fitQuality
            var high: Int = oversizeQuality

            if (high - low > 1) {
                while (high - low > 1) {
                    val mid = (low + high) / 2
                    val probe = testQuality(mid) ?: break
                    if (probe.sizeBytes < 0L) return SearchResult.EncodeFailure(iterations)

                    if (probe.sizeBytes <= maxBytes) {
                        updateBestFit(probe)
                        low = mid
                    } else {
                        high = mid
                    }
                }
            }
        }

        val candidate = bestFit
        if (candidate != null) {
            return buildFitResult(bitmap, candidate, iterations)
        }

        return SearchResult.NoFit(
            smallestBytesSeen = if (smallestBytesSeen == Long.MAX_VALUE) 0L else smallestBytesSeen,
            iterations = iterations
        )
    }

    private fun buildFitResult(
        bitmap: Bitmap,
        candidate: Candidate,
        iterations: Int
    ): SearchResult.Fit = SearchResult.Fit(
        image = ProcessedImage(
            bytes = candidate.bytes,
            width = bitmap.width,
            height = bitmap.height,
            qualityUsed = candidate.quality,
            fileSizeBytes = candidate.sizeBytes
        ),
        iterations = iterations
    )
}
