package com.kevin.easyphoto.util

import android.Manifest

object StoragePermissionPolicy {
    fun requiredPermissions(apiLevel: Int): List<String> {
        return if (apiLevel <= 28) {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            listOf(Manifest.permission.CAMERA)
        }
    }

    fun needsLegacyStorage(apiLevel: Int): Boolean = apiLevel <= 28
}
