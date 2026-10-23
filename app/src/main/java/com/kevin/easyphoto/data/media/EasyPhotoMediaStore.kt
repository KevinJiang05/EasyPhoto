package com.kevin.easyphoto.data.media

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture

interface EasyPhotoMediaStore {
    suspend fun createOutputOptions(
        context: Context,
        reversedHorizontal: Boolean = false,
    ): ImageCapture.OutputFileOptions
    suspend fun markSaved(context: Context, uri: Uri)
    suspend fun queryPhotos(context: Context): Result<List<EasyPhotoItem>>
    suspend fun deletePhotos(context: Context, uris: List<String>): Result<Int>
    suspend fun getPhotoStorageStats(context: Context): Result<PhotoStorageStats>
}

data class PhotoStorageStats(
    val count: Int,
    val totalBytes: Long,
)
