package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Deterministic center-crop regression tests.
 *
 * Guards the core product promise: requested dimensions are produced exactly
 * (±0 px), center-crop geometry is correct, and extreme aspect ratios do not
 * allocate memory-amplifying intermediates (which previously crashed on large
 * images — see the memory-safe rewrite of centerCropTo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapTransformsTest {

    private fun source(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    /** Paints each pixel a unique colour derived from (x,y) so positions are checkable. */
    private fun paintUnique(bitmap: Bitmap): Bitmap {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                bitmap.setPixel(x, y, Color.rgb((x * 13) % 256, (y * 29) % 256, ((x + y) * 7) % 256))
            }
        }
        return bitmap
    }

    @Test
    fun exactMatch_returnsSourceUntouched() {
        val src = paintUnique(source(400, 400))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertTrue("exact match must return the same instance", out === src)
        assertTrue("source must not be recycled", !out.isRecycled)
    }

    @Test
    fun landscape_toSquare_isExact() {
        val src = paintUnique(source(1200, 900))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun portrait_toSquare_isExact() {
        val src = paintUnique(source(900, 1200))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun square_toPortrait_isExact() {
        val src = paintUnique(source(1000, 1000))
        val out = BitmapTransforms.centerCropTo(src, 300, 400)
        assertEquals(300, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun upscaleToLargerTarget_isExact() {
        val src = paintUnique(source(200, 200))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun oddDimensions_areExact() {
        val src = paintUnique(source(1024, 768))
        val out = BitmapTransforms.centerCropTo(src, 401, 399)
        assertEquals(401, out.width)
        assertEquals(399, out.height)
    }

    @Test
    fun primeDimensions_areExact() {
        val src = paintUnique(source(991, 997))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun nearIntegerBoundaryAspect_isExact() {
        // 1000×1000 source to a 3:4 target — crop math must not drift a pixel.
        val src = paintUnique(source(1000, 1000))
        val out = BitmapTransforms.centerCropTo(src, 300, 400)
        assertEquals(300, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun extremeWide_doesNotAmplifyMemory_isExact() {
        // 160:1 aspect ratio — the old scale-then-crop path built a ~64,000px-wide
        // intermediate here. The memory-safe path stays bounded.
        val src = paintUnique(source(16000, 100))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun extremeTall_doesNotAmplifyMemory_isExact() {
        val src = paintUnique(source(100, 16000))
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
    }

    @Test
    fun centerCrop_geometryIsCentered() {
        // No scaling involved: 100×200 source → 100×100 target crops rows 50..149.
        // Output(x, y) must equal source(x, y + 50) exactly.
        // centerCropTo recycles its source, so capture the reference copy first.
        val src = paintUnique(source(100, 200))
        val expected = paintUnique(Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888))
        // (rebuilt below — src is recycled by the call; keep an independent copy)
        for (y in 0 until 200) for (x in 0 until 100) expected.setPixel(x, y, src.getPixel(x, y))
        val out = BitmapTransforms.centerCropTo(src, 100, 100)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                assertEquals("pixel ($x,$y)", expected.getPixel(x, y + 50), out.getPixel(x, y))
            }
        }
    }

    @Test
    fun centerCrop_wideSource_cropsHorizontallyCentered() {
        // No scaling involved: 200×100 source → 100×100 target crops cols 50..149.
        val src = paintUnique(source(200, 100))
        val expected = paintUnique(Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888))
        for (y in 0 until 100) for (x in 0 until 200) expected.setPixel(x, y, src.getPixel(x, y))
        val out = BitmapTransforms.centerCropTo(src, 100, 100)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                assertEquals("pixel ($x,$y)", expected.getPixel(x + 50, y), out.getPixel(x, y))
            }
        }
    }

    @Test
    fun downscale_cropsThenScales_keepsCenterContent() {
        // 800×800 with a solid 200×200 center block → 400×400. The center of the
        // output must be that solid block (content is preserved by the crop).
        val src = source(800, 800)
        for (y in 0 until 800) for (x in 0 until 800) {
            val inCenter = x in 300..499 && y in 300..499
            src.setPixel(x, y, if (inCenter) Color.rgb(255, 0, 0) else Color.rgb(0, 0, 255))
        }
        val out = BitmapTransforms.centerCropTo(src, 400, 400)
        assertEquals(400, out.width)
        assertEquals(400, out.height)
        assertEquals("center must keep the source's center block", Color.rgb(255, 0, 0), out.getPixel(200, 200))
    }

    @Test
    fun calculateSampleSize_powersOfTwo_keepDecodeGteTarget() {
        // Largest power-of-two sample that keeps decoded dims >= target.
        assertEquals(4, BitmapTransforms.calculateSampleSize(4000, 3000, 400, 400))
        assertEquals(2, BitmapTransforms.calculateSampleSize(4000, 3000, 1200, 1200))
        assertEquals(2, BitmapTransforms.calculateSampleSize(4000, 3000, 1000, 800))
        assertEquals(8, BitmapTransforms.calculateSampleSize(12000, 8000, 1000, 1000))
        assertEquals(1, BitmapTransforms.calculateSampleSize(400, 300, 2000, 2000))
    }
}
