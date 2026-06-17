package com.mrcriper.ymd.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MimeTypeGuesserTest {

    @Test fun `detects JPEG`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0x10)
        assertEquals(MimeType.JPEG, MimeType.guess(jpeg))
    }

    @Test fun `detects PNG`() {
        val png = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0, 0, 0,
        )
        assertEquals(MimeType.PNG, MimeType.guess(png))
    }

    @Test fun `returns null for unknown`() {
        val unknown = "not an image".toByteArray()
        assertNull(MimeType.guess(unknown))
    }

    @Test fun `empty input returns null`() {
        assertNull(MimeType.guess(ByteArray(0)))
    }
}
