package com.kevin.easyphoto

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.navigation.EasyPhotoNavGraph
import com.kevin.easyphoto.ui.theme.EasyPhotoTheme

class MainActivity : ComponentActivity() {
    private val softKioskHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EasyPhotoLogger(this).log("App 启动")
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            EasyPhotoTheme {
                EasyPhotoNavGraph()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        scheduleSoftKioskReturn("用户离开应用")
    }

    override fun onStop() {
        super.onStop()
        scheduleSoftKioskReturn("应用进入后台")
    }

    override fun onDestroy() {
        softKioskHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun scheduleSoftKioskReturn(reason: String) {
        val settings = EasyPhotoSettings(this)
        if (!settings.softKioskEnabled || settings.adminModeEnabled || isFinishing) return

        softKioskHandler.removeCallbacksAndMessages(null)
        SOFT_KIOSK_RETURN_DELAYS_MS.forEach { delay ->
            softKioskHandler.postDelayed({
                attemptSoftKioskReturn(reason)
            }, delay)
        }
    }

    private fun attemptSoftKioskReturn(reason: String) {
        val latestSettings = EasyPhotoSettings(this)
        if (!latestSettings.softKioskEnabled || latestSettings.adminModeEnabled || isFinishing) return

        EasyPhotoLogger(this).log("软锁定模式拉回 EasyPhoto：$reason")
        runCatching {
            val activityManager = getSystemService(ActivityManager::class.java)
            activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        }.onFailure {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    private companion object {
        val SOFT_KIOSK_RETURN_DELAYS_MS = longArrayOf(80L, 350L, 900L, 1600L)
    }
}
