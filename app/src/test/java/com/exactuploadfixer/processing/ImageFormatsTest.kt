package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for input-format detection (PR2: PNG/WebP input support).
 * Header logic is tested purely; file-based detection uses real encoded
 * bitmaps through Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageFormatsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ── Pure header logic ─────────────────────────────────────────────────────

    @Test
    fun `detectHeader identifies JPEG SOI`() {
        val header = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals(ImageFormat.JPEG, ImageFormat.detectHeader(header, header.size))
    }

    @Test
    fun `detectHeader identifies PNG signature`() {
        val header = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        )
        assertEquals(ImageFormat.PNG, ImageFormat.detectHeader(header, header.size))
    }

    @Test
    fun `detectHeader identifies WebP RIFF container`() {
        val header = "RIFF....WEBPVP8 ".toByteArray(Charsets.US_ASCII)
        assertEquals(ImageFormat.WEBP, ImageFormat.detectHeader(header, header.size))
    }

    @Test
    fun `detectHeader identifies HEIF ftyp box`() {
        val header = "....ftypheic".toByteArray(Charsets.US_ASCII)
        assertEquals(ImageFormat.HEIF, ImageFormat.detectHeader(header, header.size))
    }

    @Test
    fun `detectHeader rejects unknown and short headers`() {
        assertEquals(null, ImageFormat.detectHeader("GIF89a.......".toByteArray(), 12))
        assertEquals(null, ImageFormat.detectHeader(byteArrayOf(0xFF.toByte(), 0xD8.toByte()), 2))
    }

    @Test
    fun `detectHeader rejects a renamed GIF pretending to be JPEG beyond SOI`() {
        // GIF header does not start with SOI — must not pass
        val header = "GIF89a......".toByteArray(Charsets.US_ASCII)
        assertEquals(null, ImageFormat.detectHeader(header, header.size))
    }

    // ── File-based detection through real encoded bitmaps ─────────────────────

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `detect identifies a real PNG file`() {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val file = File(tmp.root, "test.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertEquals(ImageFormat.PNG, ImageFormat.detect(context(), android.net.Uri.fromFile(file)))
    }

    @Test
    fun `detect identifies a real JPEG file`() {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val file = File(tmp.root, "test.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(context(), android.net.Uri.fromFile(file)))
    }

    @Test
    fun `alpha capability flag is format-driven`() {
        assertTrue(ImageFormat.PNG.hasAlpha)
        assertTrue(ImageFormat.WEBP.hasAlpha)
        assertFalse(ImageFormat.JPEG.hasAlpha)
    }

    @Test
    fun `readable distinguishes unreadable uri from readable unsupported file`() {
        // A nonexistent path: provider can't open it → unreadable. This must be
        // classified as a decode problem (DecodeFailed upstream), never as
        // "unsupported format".
        val missing = android.net.Uri.fromFile(File(tmp.root, "does-not-exist.jpg"))
        assertFalse(ImageFormat.readable(context(), missing))
        assertFalse(ImageFormat.isSupported(context(), missing))

        // A real file with an UNUSABLE header: readable, but unsupported.
        val gif = File(tmp.root, "test.gif")
        FileOutputStream(gif).use { it.write("GIF89a".toByteArray()); it.write(ByteArray(8)) }
        assertTrue(ImageFormat.readable(context(), android.net.Uri.fromFile(gif)))
        assertFalse(ImageFormat.isSupported(context(), android.net.Uri.fromFile(gif)))
    }
}
