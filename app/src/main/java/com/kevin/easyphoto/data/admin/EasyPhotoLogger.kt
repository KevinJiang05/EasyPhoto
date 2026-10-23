package com.kevin.easyphoto.data.admin

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EasyPhotoLogger(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    fun log(message: String) {
        val current = readLogs()
        val entry = "${formatter.format(Date())}  $message"
        val next = (current + entry).takeLast(MAX_LINES)
        preferences.edit().putString(KEY_LINES, next.joinToString("\n")).apply()
    }

    fun readLogs(): List<String> =
        preferences.getString(KEY_LINES, null)
            ?.lines()
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun clear() {
        preferences.edit().remove(KEY_LINES).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "easy_photo_logs"
        const val KEY_LINES = "lines"
        const val MAX_LINES = 300
    }
}
