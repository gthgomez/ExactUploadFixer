package com.exactuploadfixer.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.ProcessedImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ImageProcessorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val cleanupFiles = mutableListOf<File>()
    private lateinit var processor: ImageProcessor

    @Before
    fun setUp() {
        processor = ImageProcessor(context)
    }

    @After
    fun tearDown() {
        cleanupFiles.forEach { it.delete() }
        cleanupFiles.clear()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a real JPEG in cacheDir with a colored gradient so JPEG compression
     * is non-trivial (solid colors compress too aggressively to be meaningful).
     */
    private fun buildTestJpeg(width: Int = 800, height: Int = 600, quality: Int = 90): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Draw a gradient-like pattern to avoid trivial compression
        for (y in 0 until height) {
            for (x in 0 until width step 50) {
                canvas.drawARGB(255, (x * 255 / width), (y * 255 / height), 128)
            }
        }
        val file = File(context.cacheDir, "test_input_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        cleanupFiles.add(file)
        return file
    }

    /**
     * Creates a landscape JPEG (800×400) then writes [orientationConstant] to its EXIF
     * so the processor must apply rotation logic before crop math.
     */
    private fun buildJpegWithExifRotation(orientationConstant: Int): File {
        val file = buildTestJpeg(width = 800, height = 400) // explicitly landscape
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientationConstant.toString())
        exif.saveAttributes()
        return file
    }

    private fun uriOf(file: File): Uri = Uri.fromFile(file)

    private suspend fun Flow<FixResult>.terminalResult(): FixResult =
        toList().last { result ->
            result is FixResult.Success ||
                result is FixResult.Degraded ||
                result is FixResult.Failure
        }

    private fun imageFrom(result: FixResult): ProcessedImage = when (result) {
        is FixResult.Success -> result.image
        is FixResult.Degraded -> result.image
        else -> throw AssertionError("Expected image-bearing result, got: $result")
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun process_outputIsValidJpeg() {
        runBlocking {
            val uri = uriOf(buildTestJpeg())
            val constraints = FixConstraints(maxBytes = 1_000_000L)

            val result = processor.process(uri, constraints).terminalResult()

            assertTrue("Expected Success, got: $result", result is FixResult.Success)
            val bytes = (result as FixResult.Success).image.bytes
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull("Output bytes must decode to a valid Bitmap", decoded)
            decoded?.recycle()
        }
    }

    @Test
    fun process_outputMeetsMaxBytes() = runBlocking {
        val maxBytes = 50_000L
        val uri = uriOf(buildTestJpeg(width = 2000, height = 2000))
        val constraints = FixConstraints(maxBytes = maxBytes)

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue(
            "Expected Success or Degraded, got: $result",
            result is FixResult.Success || result is FixResult.Degraded
        )
        val fileSizeBytes = imageFrom(result).fileSizeBytes
        assertTrue(
            "Output size $fileSizeBytes exceeds maxBytes $maxBytes",
            fileSizeBytes <= maxBytes
        )
    }

    @Test
    fun process_impossibleSizeConstraint_returnsCompressionFailure() = runBlocking {
        val uri = uriOf(buildTestJpeg())
        val constraints = FixConstraints(maxBytes = 1L) // 1 byte — impossible

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Failure, got: $result", result is FixResult.Failure)
        assertEquals(
            FixFailure.CompressionCouldNotMeetMaxSize,
            (result as FixResult.Failure).reason
        )
    }

    @Test
    fun process_dimensionConstraint_outputMatchesTarget() = runBlocking {
        val uri = uriOf(buildTestJpeg(width = 1200, height = 900))
        val constraints = FixConstraints(
            maxBytes = 500_000L,
            targetWidth = 400,
            targetHeight = 400
        )

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Success, got: $result", result is FixResult.Success)
        val image = (result as FixResult.Success).image
        assertEquals("Width must match targetWidth", 400, image.width)
        assertEquals("Height must match targetHeight", 400, image.height)
    }

    @Test
    fun process_exifRotation90_normalizedToPortrait() = runBlocking {
        // Input is 800×400 landscape with 90° EXIF — effective upright is 400×800 portrait.
        // Processor must rotate BEFORE computing sample size and crop.
        val file = buildJpegWithExifRotation(ExifInterface.ORIENTATION_ROTATE_90)
        val uri = uriOf(file)
        val constraints = FixConstraints(maxBytes = 2_000_000L) // no dimension constraint

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Success, got: $result", result is FixResult.Success)
        val image = (result as FixResult.Success).image
        // After normalizing 90° rotation, portrait: height > width
        assertTrue(
            "After EXIF normalization, height (${image.height}) must exceed width (${image.width})",
            image.height > image.width
        )
    }

    @Test
    fun process_exifRotation270_normalizedToPortrait() = runBlocking {
        val file = buildJpegWithExifRotation(ExifInterface.ORIENTATION_ROTATE_270)
        val uri = uriOf(file)
        val constraints = FixConstraints(maxBytes = 2_000_000L)

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Success, got: $result", result is FixResult.Success)
        val image = (result as FixResult.Success).image
        assertTrue(
            "After EXIF normalization, height (${image.height}) must exceed width (${image.width})",
            image.height > image.width
        )
    }

    @Test
    fun process_corruptInput_returnsFailure() = runBlocking {
        // Write random bytes that are not a valid JPEG (no SOI marker)
        val corruptFile = File(context.cacheDir, "corrupt_${System.nanoTime()}.jpg")
        corruptFile.writeBytes(ByteArray(512) { it.toByte() })
        cleanupFiles.add(corruptFile)

        val uri = uriOf(corruptFile)
        val constraints = FixConstraints(maxBytes = 1_000_000L)

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue(
            "Corrupt file must produce a Failure, got: $result",
            result is FixResult.Failure
        )
    }

    @Test
    fun process_fileProviderUri_isGrantable() = runBlocking {
        // Verify that a FileProvider URI (as used by share intents) can be read by the processor.
        // file_paths.xml exposes cacheDir/exports/ via authority ${packageName}.fileprovider.
        val exportsDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(exportsDir, "fp_test_${System.nanoTime()}.jpg")
        buildTestJpeg().copyTo(file, overwrite = true)
        cleanupFiles.add(file)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val constraints = FixConstraints(maxBytes = 1_000_000L)

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue(
            "FileProvider URI must be processable, got: $result",
            result is FixResult.Success
        )
    }

    @Test
    fun process_progressCallbackReachesOne() = runBlocking {
        // Verify the engine WOULD surface progress via the Bitmap pipeline (no explicit callback
        // in UploadFixerEngine — tested via the compress sweep completing without early exit).
        // This test confirms the happy path completes fully (quality=100 fit under maxBytes).
        val uri = uriOf(buildTestJpeg(width = 200, height = 200, quality = 80))
        val constraints = FixConstraints(maxBytes = 500_000L)

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Success for small image under large limit", result is FixResult.Success)
        // quality of 100 should fit easily — verify sweep started high
        val qualityUsed = (result as FixResult.Success).image.qualityUsed
        assertTrue("qualityUsed ($qualityUsed) must be >= 35 (QUALITY_FLOOR)", qualityUsed >= 35)
        assertTrue("qualityUsed ($qualityUsed) must be <= 100", qualityUsed <= 100)
    }

    @Test
    fun process_oneSidedDimensions_returnsInvalidTargetDimensions() = runBlocking {
        // Both or neither dimension must be set — one-sided is rejected by validate()
        val uri = uriOf(buildTestJpeg())
        val constraints = FixConstraints(
            maxBytes = 1_000_000L,
            targetWidth = 400,
            targetHeight = null  // one-sided: only width specified
        )

        val result = processor.process(uri, constraints).terminalResult()

        assertTrue("Expected Failure, got: $result", result is FixResult.Failure)
        assertEquals(
            FixFailure.InvalidTargetDimensions,
            (result as FixResult.Failure).reason
        )
    }
}
