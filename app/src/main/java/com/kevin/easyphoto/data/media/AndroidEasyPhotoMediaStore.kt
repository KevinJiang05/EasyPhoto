package com.kevin.easyphoto.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidEasyPhotoMediaStore(
    private val fileNameFormatter: PhotoFileNameFormatter = PhotoFileNameFormatter(),
) : EasyPhotoMediaStore {
    override suspend fun createOutputOptions(
        context: Context,
        reversedHorizontal: Boolean,
    ): ImageCapture.OutputFileOptions =
        withContext(Dispatchers.IO) {
            val name = fileNameFormatter.format()
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = reversedHorizontal
            }
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, EASY_PHOTO_RELATIVE_PATH)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }
                ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values,
                ).setMetadata(metadata).build()
            } else {
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val directory = File(pictures, EASY_PHOTO_FOLDER)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, name)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }
                ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values,
                ).setMetadata(metadata).build()
            }
        }

    override suspend fun markSaved(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val finalName = fileNameFormatter.format()
            val now = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                    put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                    put(MediaStore.Images.Media.DATE_TAKEN, now)
                }
                context.contentResolver.update(uri, values, null, null)
            } else {
                val oldPath = context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media.DATA),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                val oldFile = oldPath?.let(::File)
                val finalFile = oldFile?.parentFile?.let { File(it, finalName) }
                if (oldFile != null && finalFile != null && oldFile != finalFile) {
                    oldFile.renameTo(finalFile)
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                        put(MediaStore.Images.Media.DATA, finalFile.absolutePath)
                        put(MediaStore.Images.Media.DATE_TAKEN, now)
                    }
                    context.contentResolver.update(uri, values, null, null)
                }
            }
        }
    }

    override suspend fun queryPhotos(context: Context): Result<List<EasyPhotoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val projection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
            )
            if (Build.VERSION.SDK_INT >= 29) {
                projection += MediaStore.Images.Media.RELATIVE_PATH
            } else {
                projection += MediaStore.Images.Media.DATA
            }

            val selection: String
            val selectionArgs: Array<String>
            if (Build.VERSION.SDK_INT >= 29) {
                selection = "${MediaStore.Images.Media.RELATIVE_PATH}=? AND ${MediaStore.Images.Media.MIME_TYPE}=?"
                selectionArgs = arrayOf(EASY_PHOTO_RELATIVE_PATH, "image/jpeg")
            } else {
                val path = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    EASY_PHOTO_FOLDER,
                ).absolutePath + File.separator
                selection = "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE}=?"
                selectionArgs = arrayOf("$path%", "image/jpeg")
            }

            val photos = mutableListOf<EasyPhotoItem>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val takenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val addedSeconds = cursor.getLong(addedIndex)
                    photos += EasyPhotoItem(
                        id = id,
                        uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString(),
                        displayName = cursor.getString(nameIndex).orEmpty(),
                        takenAtMillis = cursor.getLong(takenIndex),
                        addedAtMillis = addedSeconds * 1000L,
                    )
                }
            }
            GalleryPhotoSorter.sortNewestFirst(photos)
        }
    }

    override suspend fun deletePhotos(context: Context, uris: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val allowedUris = queryPhotos(context).getOrThrow().map { it.uri }.toSet()
            uris
                .filter { it in allowedUris }
                .sumOf { uri ->
                    context.contentResolver.delete(Uri.parse(uri), null, null)
                }
        }
    }

    override suspend fun getPhotoStorageStats(context: Context): Result<PhotoStorageStats> = withContext(Dispatchers.IO) {
        runCatching {
            val photos = queryPhotos(context).getOrThrow()
            val totalBytes = photos.sumOf { photo ->
                runCatching {
                    context.contentResolver.openFileDescriptor(Uri.parse(photo.uri), "r")?.use {
                        it.statSize.coerceAtLeast(0L)
                    } ?: 0L
                }.getOrDefault(0L)
            }
            PhotoStorageStats(count = photos.size, totalBytes = totalBytes)
        }
    }

    private companion object {
        const val EASY_PHOTO_FOLDER = "EasyPhoto"
        const val EASY_PHOTO_RELATIVE_PATH = "Pictures/EasyPhoto/"
    }
}
