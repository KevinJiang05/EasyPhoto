package com.kevin.easyphoto.data.admin

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HomeBackgroundCache {
    suspend fun cache(context: Context, slot: HomeBackgroundSlot, sourceUri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = File(context.filesDir, CACHE_DIRECTORY)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val output = File(directory, slot.fileName)
                context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                    requireNotNull(input) { "无法读取照片" }
                    output.outputStream().use { input.copyTo(it) }
                }
                output.absolutePath
            }
        }

    fun clear(settings: EasyPhotoSettings, slot: HomeBackgroundSlot) {
        when (slot) {
            HomeBackgroundSlot.Rear -> settings.rearHomeBackgroundPath = null
            HomeBackgroundSlot.Front -> settings.frontHomeBackgroundPath = null
        }
    }

    private const val CACHE_DIRECTORY = "home-backgrounds"
}

enum class HomeBackgroundSlot(val fileName: String) {
    Rear("rear.jpg"),
    Front("front.jpg"),
}
