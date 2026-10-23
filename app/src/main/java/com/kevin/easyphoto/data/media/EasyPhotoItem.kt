package com.kevin.easyphoto.data.media

data class EasyPhotoItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val takenAtMillis: Long,
    val addedAtMillis: Long,
)
