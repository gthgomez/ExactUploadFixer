package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Deterministic file-size boundary tests for the JPEG quality search.
 *
 * Guards the core guarantee: any [JpegCompression.SearchResult.Fit] satisfies
 * fileSizeBytes <= requested maxBytes, the quality floor is respected, iterations
 * are bounded, impossible targets fail explicitly (NoFit), and identical inputs
 * produce identical outputs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JpegCompressionTest {

    private fun buildBitmap(width: Int = 800, height: Int = 600): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb(x * 255 / width, y * 255 / height, (x + y) * 255 / (width + height)))
            }
        }
        return bmp
    }

    private fun measuredSizeAt(bitmap: Bitmap, quality: Int): Long {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.size().toLong()
    }

    private fun search(
        bitmap: Bitmap,
        maxBytes: Long,
        minBytes: Long = 0L,
        maxIterations: Int = 12
    ): JpegCompression.SearchResult =
        runBlocking {
            JpegCompression.searchUnderMaxSize(bitmap, maxBytes, minBytes, maxIterations) {}
        }

    private fun fitOf(result: JpegCompression.SearchResult): JpegCompression.SearchResult.Fit =
        result as JpegCompression.SearchResult.Fit

    // ── Bounded ranges: published minimum AND maximum (e.g. 54 KB–10 MB) ──────

    @Test
    fun fit_respectsAPublishedMinimumSize() {
        val bitmap = buildBitmap()
        val q100Size = measuredSizeAt(bitmap, 100)
        val minBytes = q100Size / 2
        val result = search(bitmap, maxBytes = 1_000_000L, minBytes = minBytes)
        assertTrue("expected Fit, got: $result", result is JpegCompression.SearchResult.Fit)
        val fit = fitOf(result)
        assertTrue(
            "fit ${fit.image.fileSizeBytes} must be >= minBytes $minBytes",
            fit.image.fileSizeBytes >= minBytes
        )
    }

    @Test
    fun noFit_reportsBelowMinimumDistinctly() {
        val bitmap = buildBitmap(120, 90)
        val q35Size = measuredSizeAt(bitmap, 35)
        // A minimum above even the smallest encodable size is unreachable
        val result = search(bitmap, maxBytes = 10_000_000L, minBytes = q35Size + 1_000_000L)
        assertTrue("expected NoFit, got: $result", result is JpegCompression.SearchResult.NoFit)
        val noFit = result as JpegCompression.SearchResult.NoFit
        assertTrue(
            "must be flagged as below-minimum so the engine can raise the right failure",
            noFit.belowMinimumSeen
        )
    }

    @Test
    fun narrowBoundedWindow_findsIntermediateQuality() {
        // Audit P2: when one anchor overshoots max and the next lands under min,
        // the search must resolve the belowMin↔oversize bracket instead of
        // giving up. Fixture: measure sizes at the anchor/intermediate qualities
        // and place BOTH bounds strictly between two adjacent anchor sizes so
        // only an off-anchor quality satisfies the range.
        val bitmap = buildBitmap(400, 300)
        val sizeAt = { q: Int -> measuredSizeAt(bitmap, q) }
        val s100 = sizeAt(100)
        val s96 = sizeAt(96)
        val s92 = sizeAt(92)
        org.junit.Assume.assumeTrue(
            "fixture requires strictly increasing sizes at 92<96<100: $s92<$s96<$s100",
            s92 < s96 && s96 < s100
        )
        // max just under q100 (anchor 100 oversizes), min just above q92
        // (anchor 92 lands below min) — only the 92..100 interior can fit.
        val maxBytes = s100 - 1
        val minBytes = s92 + 1
        org.junit.Assume.assumeTrue("interior must be non-empty", s96 <= maxBytes && s96 >= minBytes)

        val result = search(bitmap, maxBytes = maxBytes, minBytes = minBytes)
        assertTrue("expected Fit in the narrow window, got: $result", result is JpegCompression.SearchResult.Fit)
        val fit = fitOf(result)
        assertTrue(fit.image.fileSizeBytes >= minBytes)
        assertTrue(fit.image.fileSizeBytes <= maxBytes)
        assertTrue("fit must use a quality above the below-min anchor", fit.image.qualityUsed > 92)
    }

    @Test
    fun noFit_withoutMinimumIsNotFlaggedBelowMinimum() {
        // Tiny maxBytes: no fit because everything is TOO BIG, not too small
        val bitmap = buildBitmap(120, 90)
        val result = search(bitmap, maxBytes = 500L)
        assertTrue(result is JpegCompression.SearchResult.NoFit)
        assertTrue(!(result as JpegCompression.SearchResult.NoFit).belowMinimumSeen)
    }

    // ── Core guarantee: never exceed the requested maxBytes ───────────────────

    @Test
    fun fit_neverExceedsRequestedMaxBytes() {
        val bitmap = buildBitmap()
        for (maxBytes in listOf(5_000L, 20_000L, 60_000L, 120_000L, 300_000L, 600_000L)) {
            val result = search(bitmap, maxBytes)
            assertTrue("expected Fit for maxBytes=$maxBytes, got: $result", result is JpegCompression.SearchResult.Fit)
            val fit = fitOf(result)
            assertTrue(
                "maxBytes=$maxBytes: output ${fit.image.fileSizeBytes} must be <= limit",
                fit.image.fileSizeBytes <= maxBytes
            )
        }
    }

    // ── Quality floor ─────────────────────────────────────────────────────────

    @Test
    fun fit_respectsQualityFloorAndCeiling() {
        val bitmap = buildBitmap()
        for (maxBytes in listOf(5_000L, 60_000L, 600_000L)) {
            val fit = fitOf(search(bitmap, maxBytes))
            assertTrue("quality ${fit.image.qualityUsed} must be >= floor 35", fit.image.qualityUsed >= JpegCompression.QUALITY_FLOOR)
            assertTrue("quality ${fit.image.qualityUsed} must be <= 100", fit.image.qualityUsed <= JpegCompression.QUALITY_MAX)
        }
    }

    // ── Exact boundary / one-byte-over ────────────────────────────────────────

    @Test
    fun exactBoundaryAtQualityFloor_fits() {
        // maxBytes == the exact size produced at the floor quality is feasible.
        val bitmap = buildBitmap()
        val floorSize = measuredSizeAt(bitmap, JpegCompression.QUALITY_FLOOR)
        val result = search(bitmap, floorSize)
        assertTrue("expected Fit at exact floor boundary, got: $result", result is JpegCompression.SearchResult.Fit)
        assertTrue(fitOf(result).image.fileSizeBytes <= floorSize)
    }

    @Test
    fun oneByteUnderQualityFloor_doesNotFit() {
        // floorSize is the smallest achievable size; one byte less is impossible.
        val bitmap = buildBitmap()
        val floorSize = measuredSizeAt(bitmap, JpegCompression.QUALITY_FLOOR)
        assertTrue("precondition: floor size must be > 1 byte", floorSize > 1L)
        val result = search(bitmap, floorSize - 1L)
        assertTrue(
            "one byte under the floor must yield NoFit, got: $result",
            result is JpegCompression.SearchResult.NoFit
        )
    }

    @Test
    fun impossibleTarget_returnsNoFit_withSmallestBytesSeen() {
        val bitmap = buildBitmap()
        val result = search(bitmap, 1L) // 1 byte is impossible for any JPEG
        assertTrue("expected NoFit, got: $result", result is JpegCompression.SearchResult.NoFit)
        val noFit = result as JpegCompression.SearchResult.NoFit
        assertTrue("smallestBytesSeen must be reported", noFit.smallestBytesSeen > 0L)
        assertTrue("smallestBytesSeen must exceed the impossible target", noFit.smallestBytesSeen > 1L)
    }

    // ── Convergence / iteration budget ────────────────────────────────────────

    @Test
    fun iterationsAreBoundedByMaxIterations() {
        val bitmap = buildBitmap()
        val result = search(bitmap, 60_000L, maxIterations = 12)
        assertTrue("iterations ${result.iterations} must be <= 12", result.iterations <= 12)
        // Even an impossible target must terminate within budget.
        val impossible = search(bitmap, 1L, maxIterations = 12)
        assertTrue("iterations ${impossible.iterations} must be <= 12", impossible.iterations <= 12)
    }

    @Test
    fun searchConverges_withinDefaultBudget() {
        val bitmap = buildBitmap()
        val result = search(bitmap, 60_000L)
        assertTrue(
            "converged search must produce a Fit, got: $result",
            result is JpegCompression.SearchResult.Fit
        )
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun sameInput_producesIdenticalFit() {
        val bitmap = buildBitmap()
        val a = fitOf(search(bitmap, 60_000L))
        val b = fitOf(search(bitmap, 60_000L))
        assertEquals("quality must be identical", a.image.qualityUsed, b.image.qualityUsed)
        assertEquals("size must be identical", a.image.fileSizeBytes, b.image.fileSizeBytes)
        assertTrue("bytes must be identical", a.image.bytes.contentEquals(b.image.bytes))
    }

    @Test
    fun largerLimit_prefersEqualOrHigherQuality() {
        val bitmap = buildBitmap()
        val small = fitOf(search(bitmap, 60_000L)).image.qualityUsed
        val large = fitOf(search(bitmap, 600_000L)).image.qualityUsed
        assertTrue("more budget must not lower quality ($small -> $large)", large >= small)
    }
}
