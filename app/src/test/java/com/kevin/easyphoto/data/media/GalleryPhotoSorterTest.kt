package com.kevin.easyphoto.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPhotoSorterTest {
    @Test
    fun sortNewestFirst_usesTakenTimeThenAddedThenId() {
        val older = item(id = 1, taken = 10, added = 50)
        val sameTakenLowerAdded = item(id = 2, taken = 20, added = 40)
        val sameTakenHigherAdded = item(id = 3, taken = 20, added = 60)
        val newest = item(id = 4, taken = 30, added = 10)

        val sorted = GalleryPhotoSorter.sortNewestFirst(
            listOf(older, sameTakenLowerAdded, newest, sameTakenHigherAdded),
        )

        assertEquals(listOf(newest, sameTakenHigherAdded, sameTakenLowerAdded, older), sorted)
    }

    @Test
    fun sortNewestFirst_emptyListReturnsEmptyList() {
        assertTrue(GalleryPhotoSorter.sortNewestFirst(emptyList()).isEmpty())
    }

    private fun item(id: Long, taken: Long, added: Long): EasyPhotoItem =
        EasyPhotoItem(id, "content://photo/$id", "p$id.jpg", taken, added)
}
