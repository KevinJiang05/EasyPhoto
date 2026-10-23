package com.kevin.easyphoto.data.feishu

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import java.util.concurrent.TimeUnit

class FeishuPhotoScheduler(private val context: Context) {
    fun enqueue(photoUri: String) {
        val request = OneTimeWorkRequestBuilder<FeishuPhotoWorker>()
            .setInputData(Data.Builder().putString(FeishuPhotoWorker.KEY_PHOTO_URI, photoUri).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_NAME_PREFIX${photoUri.hashCode()}",
            ExistingWorkPolicy.KEEP,
            request,
        )
        FeishuDeliveryState(context).queued()
        EasyPhotoLogger(context).log("飞书通知已加入发送队列")
    }

    fun cancelPending() {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    companion object {
        const val WORK_TAG = "easyphoto_feishu_photo"
        private const val WORK_NAME_PREFIX = "easyphoto-feishu-photo-"
    }
}
