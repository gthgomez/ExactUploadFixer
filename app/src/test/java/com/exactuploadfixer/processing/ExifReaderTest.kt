package com.exactuploadfixer.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies ExifReader maps every EXIF orientation constant to the correct
 * correction transform (mirror + clockwise rotation). Combined with
 * BitmapTransformsExifTest this covers the full orientation pipeline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExifReaderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cleanup = mutableListOf<File>()

    @After
    fun tearDown() {
        cleanup.forEach { it.delete() }
        cleanup.clear()
    }

    private fun jpegWithOrientation(orientation: Int): Uri {
        val file = File(context.cacheDir, "exif_${orientation}_${System.nanoTime()}.jpg")
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        bmp.recycle()
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        exif.saveAttributes()
        cleanup += file
        return Uri.fromFile(file)
    }

    private fun transformFor(orientation: Int): ExifTransform =
        ExifReader.readTransform(context, jpegWithOrientation(orientation))

    @Test
    fun normal_isIdentity() {
        assertEquals(ExifTransform(false, 0f), transformFor(ExifInterface.ORIENTATION_NORMAL))
    }

    @Test
    fun flipHorizontal_isMirrorOnly() {
        assertEquals(ExifTransform(true, 0f), transformFor(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
    }

    @Test
    fun rotate180_isRotationOnly() {
        assertEquals(ExifTransform(false, 180f), transformFor(ExifInterface.ORIENTATION_ROTATE_180))
    }

    @Test
    fun flipVertical_isMirrorPlus180() {
        assertEquals(ExifTransform(true, 180f), transformFor(ExifInterface.ORIENTATION_FLIP_VERTICAL))
    }

    @Test
    fun transpose_isMirrorPlus270() {
        assertEquals(ExifTransform(true, 270f), transformFor(ExifInterface.ORIENTATION_TRANSPOSE))
    }

    @Test
    fun rotate90_isRotationOnly() {
        assertEquals(ExifTransform(false, 90f), transformFor(ExifInterface.ORIENTATION_ROTATE_90))
    }

    @Test
    fun transverse_isMirrorPlus90() {
        assertEquals(ExifTransform(true, 90f), transformFor(ExifInterface.ORIENTATION_TRANSVERSE))
    }

    @Test
    fun rotate270_isRotationOnly() {
        assertEquals(ExifTransform(false, 270f), transformFor(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun missingExif_isIdentity() {
        // A JPEG with no EXIF orientation tag defaults to NORMAL.
        val file = File(context.cacheDir, "no_exif_${System.nanoTime()}.jpg")
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        bmp.recycle()
        cleanup += file
        assertEquals(ExifTransform(false, 0f), ExifReader.readTransform(context, Uri.fromFile(file)))
    }

    @Test
    fun corruptInput_isIdentity() {
        // Random non-JPEG bytes must not throw — safe default is identity.
        val file = File(context.cacheDir, "corrupt_exif_${System.nanoTime()}.jpg")
        file.writeBytes(ByteArray(256) { it.toByte() })
        cleanup += file
        assertEquals(ExifTransform(false, 0f), ExifReader.readTransform(context, Uri.fromFile(file)))
    }
}
