package com.kevin.easyphoto.feature.gallery

object GalleryEmptyState {
    fun shouldShowEmptyState(photoCount: Int): Boolean = photoCount == 0
}
