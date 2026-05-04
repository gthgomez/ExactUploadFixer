package com.exactuploadfixer.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Handles writing processed JPEG bytes to user-chosen destinations.
 *
 * Two export paths — both require zero storage permissions:
 *   save  → ACTION_CREATE_DOCUMENT (SAF) — user picks destination, gets a content URI
 *   share → FileProvider URI → system share sheet
 *
 * Source: Gemini Phase 3 — cleanest implementation with cache cleanup.
 * Added: explicit error return on saveToUri (not silent).
 */
object ExportManager {

    private const val EXPORTS_SUBDIR = "exports"
    private const val PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Writes [bytes] to a temp cache file. Used as the staging area for both
     * save and share flows. Each call creates a new timestamped file.
     */
    fun createTempFile(context: Context, bytes: ByteArray): File {
        val dir = File(context.cacheDir, EXPORTS_SUBDIR).apply { mkdirs() }
        val file = File(dir, "fixed_upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    /**
     * Copies [sourceFile] content to the user-selected [destinationUri].
     * [destinationUri] comes from ActivityResultContracts.CreateDocument.
     * Returns false on any IO failure — caller should show an error message.
     */
    fun saveToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean =
        try {
            val output = context.contentResolver.openOutputStream(destinationUri) ?: return false
            output.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Creates a share Intent with a FileProvider URI for [file].
     * The Intent has FLAG_GRANT_READ_URI_PERMISSION so the receiving app
     * can read the file without any storage permission.
     */
    fun getShareIntent(context: Context, file: File): Intent {
        val authority = context.packageName + PROVIDER_AUTHORITY_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Deletes all files in the exports cache directory.
     * Call at app start (clear stale files from last session) and after export completes.
     * Gemini Phase 4 QA: "Repeat use — memory stays stable" requires this cleanup.
     */
    fun cleanUpCache(context: Context) {
        val dir = File(context.cacheDir, EXPORTS_SUBDIR)
        if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
    }
}
