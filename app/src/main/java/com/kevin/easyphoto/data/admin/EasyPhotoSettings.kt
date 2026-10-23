package com.kevin.easyphoto.data.admin

import android.content.Context

class EasyPhotoSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var defaultCamera: CameraChoice
        get() = CameraChoice.fromValue(preferences.getString(KEY_DEFAULT_CAMERA, CameraChoice.Rear.value))
        set(value) {
            preferences.edit().putString(KEY_DEFAULT_CAMERA, value.value).apply()
        }

    var homeLivePreviewEnabled: Boolean
        get() = preferences.getBoolean(KEY_HOME_LIVE_PREVIEW, false)
        set(value) {
            preferences.edit().putBoolean(KEY_HOME_LIVE_PREVIEW, value).apply()
        }

    var afterCaptureAction: AfterCaptureAction
        get() = AfterCaptureAction.fromValue(preferences.getString(KEY_AFTER_CAPTURE, AfterCaptureAction.ShowResult.value))
        set(value) {
            preferences.edit().putString(KEY_AFTER_CAPTURE, value.value).apply()
        }

    var flashMode: FlashMode
        get() = FlashMode.fromValue(preferences.getString(KEY_FLASH_MODE, FlashMode.Off.value))
        set(value) {
            preferences.edit().putString(KEY_FLASH_MODE, value.value).apply()
        }

    var frontCameraMirrorEnabled: Boolean
        get() = preferences.getBoolean(KEY_FRONT_CAMERA_MIRROR, true)
        set(value) {
            preferences.edit().putBoolean(KEY_FRONT_CAMERA_MIRROR, value).apply()
        }

    var galleryPreviewCount: Int
        get() = preferences.getInt(KEY_GALLERY_PREVIEW_COUNT, 15)
        set(value) {
            preferences.edit().putInt(KEY_GALLERY_PREVIEW_COUNT, value.coerceIn(6, 15)).apply()
        }

    var newestFirst: Boolean
        get() = preferences.getBoolean(KEY_NEWEST_FIRST, true)
        set(value) {
            preferences.edit().putBoolean(KEY_NEWEST_FIRST, value).apply()
        }

    var galleryColumnCount: Int
        get() = preferences.getInt(KEY_GALLERY_COLUMN_COUNT, 2).coerceIn(2, 3)
        set(value) {
            preferences.edit().putInt(KEY_GALLERY_COLUMN_COUNT, value.coerceIn(2, 3)).apply()
        }

    var viewerArrowButtonsEnabled: Boolean
        get() = preferences.getBoolean(KEY_VIEWER_ARROW_BUTTONS, true)
        set(value) {
            preferences.edit().putBoolean(KEY_VIEWER_ARROW_BUTTONS, value).apply()
        }

    var viewerZoomEnabled: Boolean
        get() = preferences.getBoolean(KEY_VIEWER_ZOOM, true)
        set(value) {
            preferences.edit().putBoolean(KEY_VIEWER_ZOOM, value).apply()
        }

    var hapticFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) {
            preferences.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
        }

    var soundFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_SOUND_FEEDBACK, true)
        set(value) {
            preferences.edit().putBoolean(KEY_SOUND_FEEDBACK, value).apply()
        }

    var adminModeEnabled: Boolean
        get() = preferences.getBoolean(KEY_ADMIN_MODE, false)
        set(value) {
            preferences.edit().putBoolean(KEY_ADMIN_MODE, value).apply()
        }

    val hasAdminPin: Boolean
        get() = preferences.contains(KEY_ADMIN_PIN_HASH) && preferences.contains(KEY_ADMIN_PIN_SALT)

    fun setInitialAdminPin(pin: String) {
        check(!hasAdminPin)
        val salt = AdminPinPolicy.createSalt()
        val hash = AdminPinPolicy.hash(pin, salt)
        preferences.edit()
            .putString(KEY_ADMIN_PIN_SALT, salt)
            .putString(KEY_ADMIN_PIN_HASH, hash)
            .apply()
    }

    fun verifyAdminPin(pin: String): Boolean {
        val salt = preferences.getString(KEY_ADMIN_PIN_SALT, null) ?: return false
        val hash = preferences.getString(KEY_ADMIN_PIN_HASH, null) ?: return false
        return AdminPinPolicy.matches(pin, salt, hash)
    }

    var softKioskEnabled: Boolean
        get() = preferences.getBoolean(KEY_SOFT_KIOSK, false)
        set(value) {
            preferences.edit().putBoolean(KEY_SOFT_KIOSK, value).apply()
        }

    var feishuNotificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEISHU_NOTIFICATIONS_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_FEISHU_NOTIFICATIONS_ENABLED, value).apply()
        }

    var rearHomeBackgroundPath: String?
        get() = preferences.getString(KEY_REAR_HOME_BACKGROUND, null)
        set(value) {
            preferences.edit().putString(KEY_REAR_HOME_BACKGROUND, value).apply()
        }

    var frontHomeBackgroundPath: String?
        get() = preferences.getString(KEY_FRONT_HOME_BACKGROUND, null)
        set(value) {
            preferences.edit().putString(KEY_FRONT_HOME_BACKGROUND, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "easy_photo_settings"
        const val KEY_DEFAULT_CAMERA = "default_camera"
        const val KEY_HOME_LIVE_PREVIEW = "home_live_preview"
        const val KEY_AFTER_CAPTURE = "after_capture"
        const val KEY_FLASH_MODE = "flash_mode"
        const val KEY_FRONT_CAMERA_MIRROR = "front_camera_mirror"
        const val KEY_GALLERY_PREVIEW_COUNT = "gallery_preview_count"
        const val KEY_NEWEST_FIRST = "newest_first"
        const val KEY_GALLERY_COLUMN_COUNT = "gallery_column_count"
        const val KEY_VIEWER_ARROW_BUTTONS = "viewer_arrow_buttons"
        const val KEY_VIEWER_ZOOM = "viewer_zoom"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        const val KEY_SOUND_FEEDBACK = "sound_feedback"
        const val KEY_ADMIN_MODE = "admin_mode"
        const val KEY_ADMIN_PIN_SALT = "admin_pin_salt"
        const val KEY_ADMIN_PIN_HASH = "admin_pin_hash"
        const val KEY_SOFT_KIOSK = "soft_kiosk"
        const val KEY_FEISHU_NOTIFICATIONS_ENABLED = "feishu_notifications_enabled"
        const val KEY_REAR_HOME_BACKGROUND = "rear_home_background"
        const val KEY_FRONT_HOME_BACKGROUND = "front_home_background"
    }
}

enum class CameraChoice(val value: String) {
    Rear("rear"),
    Front("front");

    companion object {
        fun fromValue(value: String?): CameraChoice = entries.firstOrNull { it.value == value } ?: Rear
    }
}

enum class AfterCaptureAction(val value: String) {
    ShowResult("show_result"),
    ReturnToCamera("return_to_camera");

    companion object {
        fun fromValue(value: String?): AfterCaptureAction = entries.firstOrNull { it.value == value } ?: ShowResult
    }
}

enum class FlashMode(val value: String) {
    Off("off"),
    On("on"),
    Auto("auto");

    companion object {
        fun fromValue(value: String?): FlashMode = entries.firstOrNull { it.value == value } ?: Off
    }
}
