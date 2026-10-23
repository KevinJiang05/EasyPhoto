package com.kevin.easyphoto.data.media

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

class PhotoFileNameFormatter {
    private val formatter = DateTimeFormatter.ofPattern("'EasyPhoto_'yyyyMMdd_HHmmss_SSS'.jpg'")
    private val lastMillis = AtomicLong(-1L)

    fun format(now: LocalDateTime = LocalDateTime.now()): String {
        val baseName = now.format(formatter)
        val currentMillis = timestampKey(now)
        val previous = lastMillis.getAndSet(currentMillis)
        return if (previous == currentMillis) {
            baseName.replace(".jpg", "_1.jpg")
        } else {
            baseName
        }
    }

    private fun timestampKey(now: LocalDateTime): Long {
        return (((now.year * 100L + now.monthValue) * 100L + now.dayOfMonth) * 100L + now.hour) *
            10000000L + now.minute * 100000L + now.second * 1000L + now.nano / 1_000_000L
    }
}
