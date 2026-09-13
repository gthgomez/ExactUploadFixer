package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel-level verification that EXIF orientation correction (including the
 * mirrored orientations FLIP_HORIZONTAL / FLIP_VERTICAL / TRANSPOSE / TRANSVERSE)
 * produces the image the user sees — not a mirrored or mis-rotated variant.
 *
 * Deterministic: each source pixel has a unique colour, and the expected output
 * is computed from the EXIF semantics and compared pixel-by-pixel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapTransformsExifTest {

    private val SRC_W = 4
    private val SRC_H = 2

    private fun paintSource(): Bitmap {
        val bmp = Bitmap.createBitmap(SRC_W, SRC_H, Bitmap.Config.ARGB_8888)
        for (y in 0 until SRC_H) {
            for (x in 0 until SRC_W) {
                bmp.setPixel(x, y, Color.rgb(40 * x + 10, 60 * y + 20, 128))
            }
        }
        return bmp
    }

    /** Builds the expected output from the EXIF semantics (see formulas below). */
    private fun buildExpected(
        src: Bitmap,
        outW: Int,
        outH: Int,
        index: (x: Int, y: Int) -> Pair<Int, Int>
    ): Bitmap {
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                val (sx, sy) = index(x, y)
                out.setPixel(x, y, src.getPixel(sx, sy))
            }
        }
        return out
    }

    private fun assertBitmapEquals(expected: Bitmap, actual: Bitmap, label: String) {
        assertEquals("$label: width", expected.width, actual.width)
        assertEquals("$label: height", expected.height, actual.height)
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                assertEquals(
                    "$label: pixel ($x,$y) expected=${expected.getPixel(x, y)} actual=${actual.getPixel(x, y)}",
                    expected.getPixel(x, y),
                    actual.getPixel(x, y)
                )
            }
        }
    }

    private fun apply(transform: ExifTransform): Bitmap {
        val src = paintSource()
        val out = BitmapTransforms.applyOrientation(src, transform)
        if (out !== src) src.recycle()
        return out
    }

    @Test
    fun normal_identity_isUnchanged() {
        val src = paintSource()
        val out = BitmapTransforms.applyOrientation(src, ExifTransform(false, 0f))
        assertBitmapEquals(src, out, "normal")
    }

    @Test
    fun flipHorizontal_mirrorsLeftRight() {
        val out = apply(ExifTransform(mirrorHorizontal = true, rotationDegrees = 0f))
        // out(x,y) = in(W-1-x, y)
        val expected = buildExpected(paintSource(), SRC_W, SRC_H) { x, y -> (SRC_W - 1 - x) to y }
        assertBitmapEquals(expected, out, "flipHorizontal")
    }

    @Test
    fun rotate180_invertsBothAxes() {
        val out = apply(ExifTransform(false, 180f))
        // out(x,y) = in(W-1-x, H-1-y)
        val expected = buildExpected(paintSource(), SRC_W, SRC_H) { x, y -> (SRC_W - 1 - x) to (SRC_H - 1 - y) }
        assertBitmapEquals(expected, out, "rotate180")
    }

    @Test
    fun flipVertical_mirrorsTopBottom() {
        val out = apply(ExifTransform(mirrorHorizontal = true, rotationDegrees = 180f))
        // out(x,y) = in(x, H-1-y)
        val expected = buildExpected(paintSource(), SRC_W, SRC_H) { x, y -> x to (SRC_H - 1 - y) }
        assertBitmapEquals(expected, out, "flipVertical")
    }

    @Test
    fun transpose_swapsAxes() {
        // EXIF orientation 5 (TRANSPOSE) = mirror horizontal + rotate 270° CW.
        val out = apply(ExifTransform(mirrorHorizontal = true, rotationDegrees = 270f))
        // out(x,y) = in(y,x) — output is H×W
        val expected = buildExpected(paintSource(), SRC_H, SRC_W) { x, y -> y to x }
        assertBitmapEquals(expected, out, "transpose")
    }

    @Test
    fun rotate90_clockwise_isExact() {
        val out = apply(ExifTransform(false, 90f))
        // out(x,y) = in(y, H-1-x) — output is H×W
        val expected = buildExpected(paintSource(), SRC_H, SRC_W) { x, y -> y to (SRC_H - 1 - x) }
        assertBitmapEquals(expected, out, "rotate90")
    }

    @Test
    fun transverse_flipsAntiDiagonal() {
        // EXIF orientation 7 (TRANSVERSE) = mirror horizontal + rotate 90° CW.
        val out = apply(ExifTransform(mirrorHorizontal = true, rotationDegrees = 90f))
        // out(x,y) = in(W-1-y, H-1-x) — output is H×W
        val expected = buildExpected(paintSource(), SRC_H, SRC_W) { x, y -> (SRC_W - 1 - y) to (SRC_H - 1 - x) }
        assertBitmapEquals(expected, out, "transverse")
    }

    @Test
    fun rotate270_counterClockwise_isExact() {
        val out = apply(ExifTransform(false, 270f))
        // out(x,y) = in(W-1-y, x) — output is H×W
        val expected = buildExpected(paintSource(), SRC_H, SRC_W) { x, y -> (SRC_W - 1 - y) to x }
        assertBitmapEquals(expected, out, "rotate270")
    }

    @Test
    fun orientationSwapsDimensions_isTrueOnlyFor90And270() {
        assertEquals(true, BitmapTransforms.orientationSwapsDimensions(90f))
        assertEquals(true, BitmapTransforms.orientationSwapsDimensions(270f))
        assertEquals(false, BitmapTransforms.orientationSwapsDimensions(0f))
        assertEquals(false, BitmapTransforms.orientationSwapsDimensions(180f))
    }
}
