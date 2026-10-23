package com.kevin.easyphoto.data.feishu

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings

class FeishuPhotoWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val settings = EasyPhotoSettings(applicationContext)
        if (!settings.feishuNotificationsEnabled) return Result.success()

        val photoUri = inputData.getString(KEY_PHOTO_URI)
            ?: return fail("飞书通知失败：缺少照片地址")
        val config = FeishuSecureConfigStore(applicationContext).load()
            ?: return fail("飞书通知失败：未配置凭证")

        return try {
            FeishuDirectClient(applicationContext, config).sendPhoto(photoUri)
            FeishuDeliveryState(applicationContext).succeeded()
            EasyPhotoLogger(applicationContext).log("飞书通知发送成功")
            Result.success()
        } catch (exception: FeishuRequestException) {
            val summary = "飞书通知失败：${exception.message.orEmpty()}"
            FeishuDeliveryState(applicationContext).failed(summary)
            EasyPhotoLogger(applicationContext).log(summary)
            if (exception.retryable && runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        } catch (exception: Exception) {
            val summary = "飞书通知失败：${exception.message.orEmpty().ifBlank { "未知错误" }}"
            FeishuDeliveryState(applicationContext).failed(summary)
            EasyPhotoLogger(applicationContext).log(summary)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun fail(message: String): Result {
        FeishuDeliveryState(applicationContext).failed(message)
        EasyPhotoLogger(applicationContext).log(message)
        return Result.failure()
    }

    companion object {
        const val KEY_PHOTO_URI = "photo_uri"
        const val MAX_RETRY_ATTEMPTS = 5
    }
}
