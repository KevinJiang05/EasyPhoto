package com.kevin.easyphoto.data.media

import java.time.LocalDateTime
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFileNameFormatterTest {
    @Test
    fun format_matchesEasyPhotoPattern() {
        val name = PhotoFileNameFormatter().format(LocalDateTime.of(2026, 6, 26, 9, 8, 7, 123_000_000))

        assertTrue(name.matches(Regex("""EasyPhoto_\d{8}_\d{6}_\d{3}\.jpg""")))
        assertTrue(name.isNotBlank())
    }

    @Test
    fun format_hasCollisionFallbackForSameMillisecond() {
        val formatter = PhotoFileNameFormatter()
        val time = LocalDateTime.of(2026, 6, 26, 9, 8, 7, 123_000_000)

        val first = formatter.format(time)
        val second = formatter.format(time)

        assertNotEquals(first, second)
    }
}
