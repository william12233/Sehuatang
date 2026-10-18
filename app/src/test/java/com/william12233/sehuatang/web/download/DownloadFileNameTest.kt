package com.william12233.sehuatang.web.download

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFileNameTest {
    @Test
    fun controlCharactersAreRemovedFromDownloadName() {
        assertEquals("附件_.zip", sanitizeDownloadFileName("附件\n.zip"))
    }

    @Test
    fun blankNameGetsSafeFallback() {
        assertEquals("sehuatang-download", sanitizeDownloadFileName(" \u0000 "))
    }

    @Test
    fun longNameIsBounded() {
        assertEquals(180, sanitizeDownloadFileName("x".repeat(300)).length)
    }
}
