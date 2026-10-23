package com.kevin.easyphoto.data.feishu

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeishuDeliveryState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    val summary: String
        get() = preferences.getString(KEY_SUMMARY, "尚未发送") ?: "尚未发送"

    val updatedAt: String
        get() = preferences.getString(KEY_UPDATED_AT, "-") ?: "-"

    fun queued() = update("已加入发送队列")

    fun succeeded() = update("最近一张照片已发送")

    fun failed(message: String) = update(message.take(MAX_MESSAGE_LENGTH))

    private fun update(summary: String) {
        preferences.edit()
            .putString(KEY_SUMMARY, summary)
            .putString(KEY_UPDATED_AT, formatter.format(Date()))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "easy_photo_feishu_delivery"
        const val KEY_SUMMARY = "summary"
        const val KEY_UPDATED_AT = "updated_at"
        const val MAX_MESSAGE_LENGTH = 120
    }
}
