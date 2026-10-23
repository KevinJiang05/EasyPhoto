package com.kevin.easyphoto.feature.gallery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryEmptyStateTest {
    @Test
    fun emptyGalleryShowsTakePhotoEntry() {
        assertTrue(GalleryEmptyState.shouldShowEmptyState(0))
    }

    @Test
    fun nonEmptyGalleryDoesNotShowEmptyState() {
        assertFalse(GalleryEmptyState.shouldShowEmptyState(1))
    }
}
