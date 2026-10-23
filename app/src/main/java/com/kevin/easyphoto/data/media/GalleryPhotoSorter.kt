package com.kevin.easyphoto.data.media

object GalleryPhotoSorter {
    fun sortNewestFirst(items: List<EasyPhotoItem>): List<EasyPhotoItem> {
        return items.sortedWith(
            compareByDescending<EasyPhotoItem> { it.takenAtMillis }
                .thenByDescending { it.addedAtMillis }
                .thenByDescending { it.id },
        )
    }
}
