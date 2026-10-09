package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit tests for fit-with-padding and alpha flattening (PR2: fit-vs-crop).
 * These pin the contract that FIT_PAD preserves the whole image and produces
 * exactly the requested dimensions, and that JPEG-bound alpha is composited
 * onto white rather than silently turning black.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapTransformsFitTest {

    /** 100×50 red bitmap on transparent background: 2:1 aspect, alpha present. */
    private fun buildWideTransparentBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply { color = Color.RED }
        canvas.drawRect(0f, 0f, 100f, 50f, paint)
        return bitmap
    }

    // ── fitWithin ─────────────────────────────────────────────────────────────

    @Test
    fun `fitWithin outputs exactly the requested dimensions`() {
        val source = buildWideTransparentBitmap()
        val result = BitmapTransforms.fitWithin(source, 200, 200)
        assertEquals(200, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun `fitWithin preserves the whole image aspect ratio`() {
        val source = buildWideTransparentBitmap()
        val result = BitmapTransforms.fitWithin(source, 200, 200)
        // 2:1 source in a square frame → scaled content 200×100, centered
        // Middle row center pixel must be red (image content present)
        assertEquals(Color.RED, result.getPixel(100, 100))
        // Top and bottom bands must be padding, not stretched content
        assertEquals(Color.WHITE, result.getPixel(100, 10))
        assertEquals(Color.WHITE, result.getPixel(100, 189))
    }

    @Test
    fun `fitWithin pads with white when aspect ratios differ`() {
        val source = buildWideTransparentBitmap()
        val result = BitmapTransforms.fitWithin(source, 50, 50)
        // Content is 50×25 centered; corners are white padding
        assertEquals(Color.WHITE, result.getPixel(2, 2))
        assertEquals(Color.WHITE, result.getPixel(47, 47))
        // Content band is present
        assertEquals(Color.RED, result.getPixel(25, 25))
    }

    @Test
    fun `fitWithin with matching aspect fills the frame with content`() {
        val source = buildWideTransparentBitmap() // 2:1
        val result = BitmapTransforms.fitWithin(source, 100, 50)
        assertEquals(Color.RED, result.getPixel(2, 2))
        assertEquals(Color.RED, result.getPixel(97, 47))
    }

    // ── flattenAlphaOntoWhite ─────────────────────────────────────────────────

    @Test
    fun `flattenAlphaOntoWhite composites transparent pixels to white`() {
        // Fully transparent bitmap — a bare JPEG encode would render it black
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.TRANSPARENT)
        val flattened = BitmapTransforms.flattenAlphaOntoWhite(source)
        assertEquals(Color.WHITE, flattened.getPixel(10, 10))
    }

    @Test
    fun `flattenAlphaOntoWhite keeps opaque content unchanged`() {
        val source = buildWideTransparentBitmap() // opaque red interior
        val flattened = BitmapTransforms.flattenAlphaOntoWhite(source)
        assertEquals(Color.RED, flattened.getPixel(50, 25))
    }

    @Test
    fun `flattenAlphaOntoWhite returns opaque sources without copying`() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565)
        source.eraseColor(Color.GREEN)
        assertFalse(source.hasAlpha())
        assertTrue(BitmapTransforms.flattenAlphaOntoWhite(source) === source)
        source.recycle()
    }
}
