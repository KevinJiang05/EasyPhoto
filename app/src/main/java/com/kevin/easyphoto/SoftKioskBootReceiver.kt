package com.kevin.easyphoto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings

class SoftKioskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in bootActions) return

        val settings = EasyPhotoSettings(context)
        if (!settings.softKioskEnabled || settings.adminModeEnabled) return

        EasyPhotoLogger(context).log("软锁定模式开机自启动")
        val launchIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(launchIntent)
    }

    private companion object {
        val bootActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.huawei.intent.action.QUICKBOOT_POWERON",
        )
    }
}
