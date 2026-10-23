package com.kevin.easyphoto.util

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StoragePermissionPolicyTest {
    @Test
    fun api28_requiresCameraReadAndWriteStorage() {
        assertEquals(
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            StoragePermissionPolicy.requiredPermissions(28),
        )
    }

    @Test
    fun api29AndAbove_requiresOnlyCamera() {
        assertEquals(listOf(Manifest.permission.CAMERA), StoragePermissionPolicy.requiredPermissions(29))
        assertEquals(listOf(Manifest.permission.CAMERA), StoragePermissionPolicy.requiredPermissions(36))
    }

    @Test
    fun policyDoesNotProduceBroadMediaPermissions() {
        val all = StoragePermissionPolicy.requiredPermissions(36) + StoragePermissionPolicy.requiredPermissions(28)

        assertFalse(all.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))
        assertFalse(all.contains("android.permission.READ_MEDIA_IMAGES"))
    }
}
