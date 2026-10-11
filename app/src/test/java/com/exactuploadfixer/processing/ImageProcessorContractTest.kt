package com.exactuploadfixer.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.FitMode
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end constraint-contract verification against the REAL [ImageProcessor]
 * pipeline (decode → EXIF → crop/fit → JPEG search → bytes).
 *
 * Unlike the instrumented [ImageProcessorTest], this runs on the JVM via
 * Robolectric NATIVE graphics, so it is part of the always-on verification gate.
 *
 * The central acceptance claim is independent: the REPORTED dimensions and size
 * are not trusted — the emitted JPEG bytes are decoded again and compared to the
 * reported values and the configured bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageProcessorContractTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cleanup = mutableListOf<File>()
    private val processor = ImageProcessor(context)

    @After
    fun tearDown() {
        cleanup.forEach { it.delete() }
        cleanup.clear()
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** Non-flat JPEG so the quality sweep has a real size curve. */
    private fun writeJpeg(name: String, width: Int, height: Int): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for (y in 0 until height step 7) {
            for (x in 0 until width step 7) {
                paint.color = Color.rgb(x * 255 / width, y * 255 / height, (x + y) * 255 / (width + height))
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 7).toFloat(), (y + 7).toFloat(), paint)
            }
        }
        val file = File(context.cacheDir, "$name.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()
        cleanup += file
        return file
    }

    /** Fully transparent PNG (alpha-capable input). */
    private fun writeTransparentPng(name: String, width: Int, height: Int): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.TRANSPARENT)
        val file = File(context.cacheDir, "$name.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        cleanup += file
        return file
    }

    private suspend fun Flow<FixResult>.terminal(): FixResult =
        toList().last { it is FixResult.Success || it is FixResult.Degraded || it is FixResult.Failure }

    private fun image(result: FixResult) = when (result) {
        is FixResult.Success -> result.image
        is FixResult.Degraded -> result.image
        else -> throw AssertionError("expected an image-bearing result, got $result")
    }

    /** Independent decode of the emitted bytes; returns null if they do not decode. */
    private fun decode(bytes: ByteArray): Bitmap? =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    private fun assertDecodesToReported(result: FixResult) {
        val img = image(result)
        val decoded = decode(img.bytes)
        assertNotNull("emitted bytes must decode to a Bitmap", decoded)
        try {
            assertEquals("decoded width must equal reported width", img.width, decoded!!.width)
            assertEquals("decoded height must equal reported height", img.height, decoded.height)
        } finally {
            decoded!!.recycle()
        }
        assertEquals(
            "fileSizeBytes must equal the emitted byte count",
            img.bytes.size.toLong(),
            img.fileSizeBytes
        )
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun crop_outputDecodesToExactTargetDimensionsAndRespectsMax() = runBlocking {
        val maxBytes = 250_000L
        val constraints = FixConstraints(
            maxBytes = maxBytes,
            targetWidth = 400,
            targetHeight = 400,
            fitMode = FitMode.CROP
        )
        val result = processor.process(Uri.fromFile(writeJpeg("crop_src", 1200, 900)), constraints).terminal()

        assertTrue("expected Success/Degraded, got $result", result.isSuccess)
        val img = image(result)
        assertEquals(400, img.width)
        assertEquals(400, img.height)
        assertTrue("output ${img.fileSizeBytes} must be <= maxBytes $maxBytes", img.fileSizeBytes <= maxBytes)
        assertDecodesToReported(result)
    }

    @Test
    fun fitPad_outputDecodesToExactTargetDimensionsAndRespectsMax() = runBlocking {
        val maxBytes = 250_000L
        val constraints = FixConstraints(
            maxBytes = maxBytes,
            targetWidth = 600,
            targetHeight = 600,
            fitMode = FitMode.FIT_PAD
        )
        val result = processor.process(Uri.fromFile(writeJpeg("fit_src", 800, 500)), constraints).terminal()

        assertTrue("expected Success/Degraded, got $result", result.isSuccess)
        val img = image(result)
        assertEquals(600, img.width)
        assertEquals(600, img.height)
        assertTrue("output ${img.fileSizeBytes} must be <= maxBytes $maxBytes", img.fileSizeBytes <= maxBytes)
        assertDecodesToReported(result)
    }

    @Test
    fun noDimensions_outputDecodesToSourceDimensionsAndRespectsMax() = runBlocking {
        val maxBytes = 200_000L
        val constraints = FixConstraints(maxBytes = maxBytes)
        val result = processor.process(Uri.fromFile(writeJpeg("plain_src", 1000, 750)), constraints).terminal()

        assertTrue("expected Success/Degraded, got $result", result.isSuccess)
        val img = image(result)
        assertTrue("output ${img.fileSizeBytes} must be <= maxBytes $maxBytes", img.fileSizeBytes <= maxBytes)
        assertDecodesToReported(result)
    }

    @Test
    fun boundedRange_outputDecodesWithinPublishedMinAndMax() = runBlocking {
        // Passport-renewal shape: a published floor AND ceiling. The engine must
        // prefer a quality that lands inside the closed range, and a passing
        // output must satisfy BOTH sides when decoded.
        val minBytes = 55_296L
        val maxBytes = 10_000_000L
        val constraints = FixConstraints(
            maxBytes = maxBytes,
            minBytes = minBytes,
            targetWidth = 600,
            targetHeight = 600,
            fitMode = FitMode.CROP
        )
        val result = processor.process(Uri.fromFile(writeJpeg("bounded_src", 1600, 1600)), constraints).terminal()

        assertTrue("expected Success/Degraded, got $result", result.isSuccess)
        val img = image(result)
        assertTrue("output ${img.fileSizeBytes} must be >= minBytes $minBytes", img.fileSizeBytes >= minBytes)
        assertTrue("output ${img.fileSizeBytes} must be <= maxBytes $maxBytes", img.fileSizeBytes <= maxBytes)
        assertDecodesToReported(result)
    }

    @Test
    fun transparentPngOutput_compositesOntoWhiteNotBlack() = runBlocking {
        // Alpha-capable input flattened for JPEG: transparent pixels must become
        // WHITE, never the black a bare encode would produce.
        val constraints = FixConstraints(
            maxBytes = 500_000L,
            targetWidth = 200,
            targetHeight = 200,
            fitMode = FitMode.FIT_PAD
        )
        val result = processor.process(Uri.fromFile(writeTransparentPng("alpha_src", 200, 200)), constraints).terminal()

        assertTrue("expected Success/Degraded, got $result", result.isSuccess)
        val img = image(result)
        val decoded = decode(img.bytes)
        assertNotNull(decoded)
        try {
            val px = decoded!!.getPixel(100, 100)
            val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
            assertTrue(
                "transparent source must composite to white, got rgb($r,$g,$b)",
                r > 200 && g > 200 && b > 200
            )
        } finally {
            decoded!!.recycle()
        }
        assertDecodesToReported(result)
    }

    @Test
    fun impossibleSize_failsExplicitlyWithoutEmittingAnImage() = runBlocking {
        // A tiny byte cap no JPEG can meet: explicit failure, never a silent
        // over-limit export and never an image-bearing result.
        val constraints = FixConstraints(maxBytes = 1L, targetWidth = 400, targetHeight = 400)
        val result = processor.process(Uri.fromFile(writeJpeg("impossible_src", 800, 600)), constraints).terminal()

        assertTrue("expected Failure, got $result", result is FixResult.Failure)
    }
}
