package com.exactuploadfixer.processing

import android.content.Context
import android.net.Uri

/**
 * Input image format detection. MIME type is checked first (fast path), then
 * actual magic bytes — mislabeled files are common (e.g. a WebP renamed .jpg
 * from messaging apps). Detection decides the decode config (alpha-capable
 * formats decode to ARGB_8888 so transparency can be composited deliberately,
 * never silently black).
 *
 * Output remains JPEG in v1.1: PNG/WebP/HEIC are INPUT formats only.
 * HEIC/HEIF decoding is hardware/OS-dependent (API 26+); a decode failure is
 * an explicit FixFailure.DecodeFailed, never a crash.
 */
enum class ImageFormat(val mime: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp"),
    HEIF("image/heif");

    /** Formats that can carry an alpha channel and need ARGB_8888 + flatten. */
    val hasAlpha: Boolean get() = this != JPEG

    companion object {
        val SUPPORTED = setOf(JPEG, PNG, WEBP, HEIF)

        /**
         * True when the URI is an image this app can process. Ground truth is
         * the FILE HEADER, never the declared MIME type: mislabeled files are
         * common, and a trusted MIME would let a renamed/foreign file through
         * to the decoder. Returns false for anything else — the caller
         * surfaces an explicit unsupported-format message.
         */
        fun isSupported(context: Context, uri: Uri): Boolean = detect(context, uri) != null

        /**
         * True when the URI can be opened and yields at least one byte.
         * Distinguishes "provider/stream unreadable" (a decode problem →
         * DecodeFailed) from "readable but unknown header" (a format problem →
         * UnsupportedMimeType). A SecurityException from a revoked grant or an
         * empty cloud-provider stream is NOT a format issue and must not be
         * reported as one.
         */
        fun readable(context: Context, uri: Uri): Boolean = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.read() != -1
            } == true
        } catch (_: Exception) {
            false
        }

        /** Detected format from magic bytes, or null when unsupported/unreadable. */
        fun detect(context: Context, uri: Uri): ImageFormat? = detectByMagic(context, uri)

        private fun detectByMagic(context: Context, uri: Uri): ImageFormat? = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(12)
                var read = 0
                while (read < header.size) {
                    val n = stream.read(header, read, header.size - read)
                    if (n < 0) break
                    read += n
                }
                detectHeader(header, read)
            }
        } catch (_: Exception) {
            null
        }

        /** Visible for testing — pure byte logic. */
        internal fun detectHeader(header: ByteArray, length: Int): ImageFormat? {
            if (length >= 3 &&
                header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
            ) return JPEG
            if (length >= 8 && PNG_SIGNATURE.indices.all { header[it] == PNG_SIGNATURE[it] }) return PNG
            if (length >= 12 &&
                header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
                header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
            ) return WEBP
            // ISO-BMFF: require the ftyp box AND a known HEIF/HEVC brand —
            // bare "ftyp" would also match MP4/MOV containers.
            if (length >= 12 && String(header, 4, 4) == "ftyp") {
                val brand = String(header, 8, 4)
                if (brand in HEIF_BRANDS) return HEIF
            }
            return null
        }

        private val HEIF_BRANDS = setOf("heic", "heix", "hevc", "hevx", "mif1", "msf1")

        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
    }
}
