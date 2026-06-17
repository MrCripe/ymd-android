package com.mrcriper.ymd.domain.util

/**
 * MIME detection by magic bytes — port of [py-ref/ymd/mime_utils.py].
 */
enum class MimeType(val mime: String, val extension: String) {
    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png");

    companion object {
        private val MAGIC = listOf(
            JPEG to byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            PNG to byteArrayOf(
                0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
                0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            ),
        )

        fun guess(data: ByteArray): MimeType? = MAGIC.firstNotNullOfOrNull { (mime, magic) ->
            if (data.size >= magic.size && data.take(magic.size).toByteArray().contentEquals(magic)) mime else null
        }
    }
}
