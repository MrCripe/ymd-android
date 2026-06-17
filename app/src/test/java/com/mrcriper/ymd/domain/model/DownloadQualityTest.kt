package com.mrcriper.ymd.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadQualityTest {

    @Test fun `value maps to enum`() {
        assertEquals(DownloadQuality.LOW, DownloadQuality.from(0))
        assertEquals(DownloadQuality.NORMAL, DownloadQuality.from(1))
        assertEquals(DownloadQuality.BEST, DownloadQuality.from(2))
    }

    @Test fun `unknown value defaults to BEST`() {
        assertEquals(DownloadQuality.BEST, DownloadQuality.from(99))
        assertEquals(DownloadQuality.BEST, DownloadQuality.from(null))
    }

    @Test fun `api values match python reference`() {
        assertEquals("lq", DownloadQuality.LOW.apiValue)
        assertEquals("nq", DownloadQuality.NORMAL.apiValue)
        assertEquals("lossless", DownloadQuality.BEST.apiValue)
    }
}
