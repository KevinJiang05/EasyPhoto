package com.kevin.easyphoto.feature.admin

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.easyphoto.data.admin.AfterCaptureAction
import com.kevin.easyphoto.data.admin.CameraChoice
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.admin.FlashMode
import com.kevin.easyphoto.data.admin.HomeBackgroundCache
import com.kevin.easyphoto.data.admin.HomeBackgroundSlot
import com.kevin.easyphoto.data.feishu.FeishuDeliveryState
import com.kevin.easyphoto.data.feishu.FeishuDirectClient
import com.kevin.easyphoto.data.feishu.FeishuNotificationConfig
import com.kevin.easyphoto.data.feishu.FeishuNotificationConfigValidator
import com.kevin.easyphoto.data.feishu.FeishuPhotoScheduler
import com.kevin.easyphoto.data.feishu.FeishuSecureConfigStore
import com.kevin.easyphoto.data.media.EasyPhotoMediaStore
import com.kevin.easyphoto.data.media.PhotoStorageStats
import com.kevin.easyphoto.ui.theme.EasyPhotoDanger
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import com.kevin.easyphoto.ui.theme.EasyPhotoSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.WorkInfo
import androidx.work.WorkManager

@Composable
fun AdminScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    logger: EasyPhotoLogger,
    onBackHome: () -> Unit,
) {
    BackHandler { onBackHome() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val feishuConfigStore = remember { FeishuSecureConfigStore(context) }
    val feishuPhotoScheduler = remember { FeishuPhotoScheduler(context) }
    val feishuDeliveryState = remember { FeishuDeliveryState(context) }
    val savedFeishuConfig = remember { feishuConfigStore.load() }
    var defaultCamera by remember { mutableStateOf(settings.defaultCamera) }
    var afterCaptureAction by remember { mutableStateOf(settings.afterCaptureAction) }
    var flashMode by remember { mutableStateOf(settings.flashMode) }
    var frontCameraMirrorEnabled by remember { mutableStateOf(settings.frontCameraMirrorEnabled) }
    var galleryPreviewCount by remember { mutableStateOf(settings.galleryPreviewCount) }
    var newestFirst by remember { mutableStateOf(settings.newestFirst) }
    var galleryColumnCount by remember { mutableStateOf(settings.galleryColumnCount) }
    var viewerArrowButtonsEnabled by remember { mutableStateOf(settings.viewerArrowButtonsEnabled) }
    var viewerZoomEnabled by remember { mutableStateOf(settings.viewerZoomEnabled) }
    var hapticFeedbackEnabled by remember { mutableStateOf(settings.hapticFeedbackEnabled) }
    var soundFeedbackEnabled by remember { mutableStateOf(settings.soundFeedbackEnabled) }
    var stats by remember { mutableStateOf(PhotoStorageStats(0, 0L)) }
    var rearHomeBackgroundPath by remember { mutableStateOf(settings.rearHomeBackgroundPath) }
    var frontHomeBackgroundPath by remember { mutableStateOf(settings.frontHomeBackgroundPath) }
    var softKioskEnabled by remember { mutableStateOf(settings.softKioskEnabled) }
    var feishuNotificationsEnabled by remember { mutableStateOf(settings.feishuNotificationsEnabled) }
    var feishuAppId by remember { mutableStateOf(savedFeishuConfig?.appId.orEmpty()) }
    var feishuAppSecret by remember { mutableStateOf(savedFeishuConfig?.appSecret.orEmpty()) }
    var feishuWebhookUrl by remember { mutableStateOf(savedFeishuConfig?.webhookUrl.orEmpty()) }
    var hasSavedFeishuConfig by remember { mutableStateOf(savedFeishuConfig != null) }
    var isTestingFeishu by remember { mutableStateOf(false) }
    var feishuPendingCount by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(logger.readLogs()) }
    var logPage by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    var showClearLogConfirm by remember { mutableStateOf(false) }

    fun refreshPhotos() {
        refreshKey += 1
    }

    fun refreshFeishuQueue() {
        scope.launch {
            feishuPendingCount = withContext(Dispatchers.IO) {
                WorkManager.getInstance(context)
                    .getWorkInfosByTag(FeishuPhotoScheduler.WORK_TAG)
                    .get()
                    .count {
                        it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.BLOCKED ||
                            it.state == WorkInfo.State.RUNNING
                    }
            }
        }
    }

    fun setHomeBackground(slot: HomeBackgroundSlot, sourceUri: String) {
        scope.launch {
            HomeBackgroundCache.cache(context, slot, sourceUri)
                .onSuccess { path ->
                    when (slot) {
                        HomeBackgroundSlot.Rear -> {
                            settings.rearHomeBackgroundPath = path
                            rearHomeBackgroundPath = path
                        }
                        HomeBackgroundSlot.Front -> {
                            settings.frontHomeBackgroundPath = path
                            frontHomeBackgroundPath = path
                        }
                    }
                    logger.log("管理员设置首页${slot.label()}背景")
                    logs = logger.readLogs()
                    message = "已设置首页${slot.label()}背景"
                }
                .onFailure {
                    logger.log("管理员设置首页${slot.label()}背景失败：${it.message}")
                    logs = logger.readLogs()
                    message = "背景设置失败"
                }
        }
    }

    val rearBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            setHomeBackground(HomeBackgroundSlot.Rear, uri.toString())
        }
    }
    val frontBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            setHomeBackground(HomeBackgroundSlot.Front, uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        settings.homeLivePreviewEnabled = false
        refreshFeishuQueue()
    }

    LaunchedEffect(refreshKey) {
        mediaStore.getPhotoStorageStats(context).onSuccess {
            stats = it
        }
        logs = logger.readLogs()
    }

    LaunchedEffect(logs.size) {
        val totalPages = ((logs.size + LogPageSize - 1) / LogPageSize).coerceAtLeast(1)
        if (logPage >= totalPages) {
            logPage = totalPages - 1
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("设置", style = AdminHeadingStyle)
                Button(onClick = onBackHome) {
                    AdminButtonText("返回相册")
                }
            }
            if (message.isNotBlank()) {
                Text(message, color = EasyPhotoPrimary, style = AdminBodyStyle)
            }
        }

        item {
            AdminSection(title = "管理员模式") {
                Text("管理员模式开启后，相册页可以选择并删除照片。", style = AdminBodyStyle)
                ChoiceRow(
                    title = "软锁定模式",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = softKioskEnabled,
                    onLeft = {
                        softKioskEnabled = true
                        settings.softKioskEnabled = true
                        logger.log("开启软锁定模式")
                        logs = logger.readLogs()
                        message = "已开启软锁定模式"
                    },
                    onRight = {
                        softKioskEnabled = false
                        settings.softKioskEnabled = false
                        logger.log("关闭软锁定模式")
                        logs = logger.readLogs()
                        message = "已关闭软锁定模式"
                    },
                )
                Text(
                    "开启后，非管理员状态下离开 App 会尝试自动回到 EasyPhoto，开机后也会尝试自动启动。",
                    style = AdminSmallStyle,
                )
                Text(
                    "如 EasyPhoto 已被设为默认桌面，可进入系统设置切回华为桌面。",
                    style = AdminSmallStyle,
                )
                Button(
                    onClick = {
                        logger.log("管理员打开默认桌面设置")
                        logs = logger.readLogs()
                        context.openHomeSettings()
                    },
                ) {
                    AdminButtonText("停用默认桌面")
                }
                Button(
                    onClick = {
                        settings.adminModeEnabled = false
                        logger.log("关闭管理员模式")
                        onBackHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EasyPhotoDanger),
                ) {
                    AdminButtonText("关闭管理员模式")
                }
            }
        }

        item {
            AdminSection(title = "相机设置") {
                ChoiceRow(
                    title = "默认拍摄入口",
                    leftText = "后置",
                    rightText = "前置",
                    leftSelected = defaultCamera == CameraChoice.Rear,
                    onLeft = {
                        defaultCamera = CameraChoice.Rear
                        settings.defaultCamera = defaultCamera
                        logger.log("管理员设置默认入口：后置")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        defaultCamera = CameraChoice.Front
                        settings.defaultCamera = defaultCamera
                        logger.log("管理员设置默认入口：前置")
                        logs = logger.readLogs()
                    },
                )
                Text("首页实时预览：已停用", style = AdminBodyStyle)
                Text(
                    "Mate 20 返回拍摄页时会触发相机底层崩溃，首页不再持有相机资源。",
                    style = AdminSmallStyle,
                )
                ChoiceRow(
                    title = "拍照后动作",
                    leftText = "看结果",
                    rightText = "继续拍",
                    leftSelected = afterCaptureAction == AfterCaptureAction.ShowResult,
                    onLeft = {
                        afterCaptureAction = AfterCaptureAction.ShowResult
                        settings.afterCaptureAction = afterCaptureAction
                        logger.log("管理员设置拍照后动作：看结果")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        afterCaptureAction = AfterCaptureAction.ReturnToCamera
                        settings.afterCaptureAction = afterCaptureAction
                        logger.log("管理员设置拍照后动作：继续拍")
                        logs = logger.readLogs()
                    },
                )
                ChoiceRow(
                    title = "前置镜像",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = frontCameraMirrorEnabled,
                    onLeft = {
                        frontCameraMirrorEnabled = true
                        settings.frontCameraMirrorEnabled = true
                        logger.log("管理员开启前置镜像")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        frontCameraMirrorEnabled = false
                        settings.frontCameraMirrorEnabled = false
                        logger.log("管理员关闭前置镜像")
                        logs = logger.readLogs()
                    },
                )
                Text("闪光灯默认", style = AdminBodyStyle)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlashChoiceButton(
                        icon = "关",
                        label = "关闭",
                        selected = flashMode == FlashMode.Off,
                        onClick = {
                            flashMode = FlashMode.Off
                            settings.flashMode = flashMode
                            logger.log("管理员设置闪光灯默认：关闭")
                            logs = logger.readLogs()
                        },
                    )
                    FlashChoiceButton(
                        icon = "⚡",
                        label = "开启",
                        selected = flashMode == FlashMode.On,
                        onClick = {
                            flashMode = FlashMode.On
                            settings.flashMode = flashMode
                            logger.log("管理员设置闪光灯默认：开启")
                            logs = logger.readLogs()
                        },
                    )
                    FlashChoiceButton(
                        icon = "A",
                        label = "自动",
                        selected = flashMode == FlashMode.Auto,
                        onClick = {
                            flashMode = FlashMode.Auto
                            settings.flashMode = flashMode
                            logger.log("管理员设置闪光灯默认：自动")
                            logs = logger.readLogs()
                        },
                    )
                }
            }
        }

        item {
            AdminSection(title = "适老反馈") {
                ChoiceRow(
                    title = "按钮震动",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = hapticFeedbackEnabled,
                    onLeft = {
                        hapticFeedbackEnabled = true
                        settings.hapticFeedbackEnabled = true
                        logger.log("管理员开启按钮震动")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        hapticFeedbackEnabled = false
                        settings.hapticFeedbackEnabled = false
                        logger.log("管理员关闭按钮震动")
                        logs = logger.readLogs()
                    },
                )
                ChoiceRow(
                    title = "拍照成功提示音",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = soundFeedbackEnabled,
                    onLeft = {
                        soundFeedbackEnabled = true
                        settings.soundFeedbackEnabled = true
                        logger.log("管理员开启拍照提示音")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        soundFeedbackEnabled = false
                        settings.soundFeedbackEnabled = false
                        logger.log("管理员关闭拍照提示音")
                        logs = logger.readLogs()
                    },
                )
                Text("管理员入口：长按首页底部版本信息 4 秒。", style = AdminSmallStyle)
            }
        }

        item {
            AdminSection(title = "相册设置") {
                ChoiceRow(
                    title = "相册照片大小",
                    leftText = "大图（2列）",
                    rightText = "更多（3列）",
                    leftSelected = galleryColumnCount == 2,
                    onLeft = {
                        galleryColumnCount = 2
                        settings.galleryColumnCount = 2
                        logger.log("管理员设置相册：2列")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        galleryColumnCount = 3
                        settings.galleryColumnCount = 3
                        logger.log("管理员设置相册：3列")
                        logs = logger.readLogs()
                    },
                )
                ChoiceRow(
                    title = "全屏左右箭头",
                    leftText = "显示",
                    rightText = "隐藏",
                    leftSelected = viewerArrowButtonsEnabled,
                    onLeft = {
                        viewerArrowButtonsEnabled = true
                        settings.viewerArrowButtonsEnabled = true
                        logger.log("管理员显示全屏翻页箭头")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        viewerArrowButtonsEnabled = false
                        settings.viewerArrowButtonsEnabled = false
                        logger.log("管理员隐藏全屏翻页箭头")
                        logs = logger.readLogs()
                    },
                )
                ChoiceRow(
                    title = "全屏双指缩放",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = viewerZoomEnabled,
                    onLeft = {
                        viewerZoomEnabled = true
                        settings.viewerZoomEnabled = true
                        logger.log("管理员开启全屏双指缩放")
                        logs = logger.readLogs()
                    },
                    onRight = {
                        viewerZoomEnabled = false
                        settings.viewerZoomEnabled = false
                        logger.log("管理员关闭全屏双指缩放")
                        logs = logger.readLogs()
                    },
                )
                ChoiceRow(
                    title = "首页拼图数量",
                    leftText = "9 张",
                    rightText = "15 张",
                    leftSelected = galleryPreviewCount == 9,
                    onLeft = {
                        galleryPreviewCount = 9
                        settings.galleryPreviewCount = 9
                        logger.log("管理员设置首页拼图数量：9")
                        refreshPhotos()
                    },
                    onRight = {
                        galleryPreviewCount = 15
                        settings.galleryPreviewCount = 15
                        logger.log("管理员设置首页拼图数量：15")
                        refreshPhotos()
                    },
                )
                ChoiceRow(
                    title = "相册排序",
                    leftText = "最新在前",
                    rightText = "最旧在前",
                    leftSelected = newestFirst,
                    onLeft = {
                        newestFirst = true
                        settings.newestFirst = true
                        logger.log("管理员设置相册排序：最新在前")
                        refreshPhotos()
                    },
                    onRight = {
                        newestFirst = false
                        settings.newestFirst = false
                        logger.log("管理员设置相册排序：最旧在前")
                        refreshPhotos()
                    },
                )
                Text("保存位置：Pictures/EasyPhoto", style = AdminBodyStyle)
                Text("照片数量：${stats.count}", style = AdminBodyStyle)
                Text("占用空间：${stats.totalBytes.toReadableSize()}", style = AdminBodyStyle)
                Divider(Modifier.padding(vertical = 4.dp))
                Text("首页按钮背景", style = AdminBodyStyle)
                Text(
                    "从系统相册选择，选中后复制到 App 私有缓存，原照片删除后仍可显示。",
                    style = AdminSmallStyle,
                )
                HomeBackgroundStatusRow(
                    rearPath = rearHomeBackgroundPath,
                    frontPath = frontHomeBackgroundPath,
                    onSelectRear = { rearBackgroundPicker.launch("image/*") },
                    onSelectFront = { frontBackgroundPicker.launch("image/*") },
                    onClearRear = {
                        HomeBackgroundCache.clear(settings, HomeBackgroundSlot.Rear)
                        rearHomeBackgroundPath = null
                        logger.log("管理员清除首页后置背景")
                        logs = logger.readLogs()
                        message = "已清除后置背景"
                    },
                    onClearFront = {
                        HomeBackgroundCache.clear(settings, HomeBackgroundSlot.Front)
                        frontHomeBackgroundPath = null
                        logger.log("管理员清除首页前置背景")
                        logs = logger.readLogs()
                        message = "已清除前置背景"
                    },
                )
            }
        }

        item {
            AdminSection(title = "飞书照片通知") {
                Text(
                    "开启后，本机保存照片成功才会异步发送到指定飞书私密群；不会影响拍照和本地相册。",
                    style = AdminSmallStyle,
                )
                ChoiceRow(
                    title = "自动发送照片",
                    leftText = "开启",
                    rightText = "关闭",
                    leftSelected = feishuNotificationsEnabled,
                    onLeft = {
                        if (!hasSavedFeishuConfig) {
                            message = "请先保存飞书配置"
                        } else {
                            feishuNotificationsEnabled = true
                            settings.feishuNotificationsEnabled = true
                            logger.log("管理员开启飞书照片通知")
                            logs = logger.readLogs()
                            message = "已开启飞书照片通知"
                        }
                    },
                    onRight = {
                        feishuNotificationsEnabled = false
                        settings.feishuNotificationsEnabled = false
                        feishuPhotoScheduler.cancelPending()
                        logger.log("管理员关闭飞书照片通知并取消待发任务")
                        logs = logger.readLogs()
                        message = "已关闭飞书通知"
                        refreshFeishuQueue()
                    },
                )
                Text("配置状态：${if (hasSavedFeishuConfig) "已加密保存到本机" else "未配置"}", style = AdminSmallStyle)
                Text("最近状态：${feishuDeliveryState.summary}", style = AdminSmallStyle)
                Text("更新时间：${feishuDeliveryState.updatedAt}，待发送：$feishuPendingCount", style = AdminSmallStyle)
                OutlinedTextField(
                    value = feishuAppId,
                    onValueChange = { feishuAppId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("飞书 App ID") },
                    singleLine = true,
                    textStyle = AdminSmallStyle,
                )
                OutlinedTextField(
                    value = feishuAppSecret,
                    onValueChange = { feishuAppSecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("飞书 App Secret") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = AdminSmallStyle,
                )
                OutlinedTextField(
                    value = feishuWebhookUrl,
                    onValueChange = { feishuWebhookUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("私密群 Webhook 地址") },
                    singleLine = true,
                    textStyle = AdminSmallStyle,
                )
                Text("不在日志、截图或代码中保存凭证。Webhook 仅支持向指定群推送。", style = AdminSmallStyle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(
                        text = "保存配置",
                        onClick = {
                            val config = FeishuNotificationConfig(
                                appId = feishuAppId,
                                appSecret = feishuAppSecret,
                                webhookUrl = feishuWebhookUrl,
                            )
                            val validationMessage = FeishuNotificationConfigValidator.validate(config)
                            if (validationMessage != null) {
                                message = validationMessage
                            } else {
                                runCatching { feishuConfigStore.save(config) }
                                    .onSuccess {
                                        hasSavedFeishuConfig = true
                                        logger.log("管理员保存飞书通知配置")
                                        logs = logger.readLogs()
                                        message = "飞书配置已加密保存"
                                    }
                                    .onFailure {
                                        message = "飞书配置保存失败"
                                        logger.log("飞书配置保存失败：${it.javaClass.simpleName}")
                                        logs = logger.readLogs()
                                    }
                            }
                        },
                    )
                    CompactButton(
                        text = if (isTestingFeishu) "测试中" else "测试连接",
                        enabled = !isTestingFeishu,
                        onClick = {
                            val config = FeishuNotificationConfig(
                                appId = feishuAppId,
                                appSecret = feishuAppSecret,
                                webhookUrl = feishuWebhookUrl,
                            )
                            val validationMessage = FeishuNotificationConfigValidator.validate(config)
                            if (validationMessage != null) {
                                message = validationMessage
                            } else {
                                isTestingFeishu = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { FeishuDirectClient(context, config).testConnection() }
                                    }
                                    isTestingFeishu = false
                                    result.onSuccess {
                                        logger.log("飞书通知连接测试成功")
                                        message = "飞书连接测试成功，请查看私密群"
                                    }.onFailure {
                                        logger.log("飞书连接测试失败：${it.message.orEmpty()}")
                                        message = "飞书连接测试失败：${it.message.orEmpty()}"
                                    }
                                    logs = logger.readLogs()
                                }
                            }
                        },
                    )
                    CompactButton(
                        text = "刷新状态",
                        onClick = { refreshFeishuQueue() },
                    )
                }
                CompactButton(
                    text = "清除飞书配置",
                    enabled = hasSavedFeishuConfig,
                    onClick = {
                        feishuNotificationsEnabled = false
                        settings.feishuNotificationsEnabled = false
                        feishuPhotoScheduler.cancelPending()
                        feishuConfigStore.clear()
                        feishuAppId = ""
                        feishuAppSecret = ""
                        feishuWebhookUrl = ""
                        hasSavedFeishuConfig = false
                        logger.log("管理员清除飞书通知配置")
                        logs = logger.readLogs()
                        message = "已清除飞书配置并关闭通知"
                        refreshFeishuQueue()
                    },
                )
            }
        }

        item {
            AdminSection(title = "诊断信息") {
                Text("设备：${Build.MANUFACTURER} ${Build.MODEL}", style = AdminBodyStyle)
                Text("Android API：${Build.VERSION.SDK_INT}", style = AdminBodyStyle)
                Text("后置摄像头：${if (context.hasCamera(CameraCharacteristics.LENS_FACING_BACK)) "可用" else "不可用"}", style = AdminBodyStyle)
                Text("前置摄像头：${if (context.hasCamera(CameraCharacteristics.LENS_FACING_FRONT)) "可用" else "不可用"}", style = AdminBodyStyle)
                Button(
                    onClick = {
                        logger.log("管理员重新检测相机：后置=${context.hasCamera(CameraCharacteristics.LENS_FACING_BACK)}，前置=${context.hasCamera(CameraCharacteristics.LENS_FACING_FRONT)}")
                        logs = logger.readLogs()
                        message = "已重新检测相机"
                    },
                ) {
                    AdminButtonText("重新检测相机")
                }
            }
        }

        item {
            AdminSection(title = "日志") {
                val pagedLogs = logs.asReversed()
                val totalPages = ((pagedLogs.size + LogPageSize - 1) / LogPageSize).coerceAtLeast(1)
                val currentPage = logPage.coerceIn(0, totalPages - 1)
                val pageLogs = pagedLogs.drop(currentPage * LogPageSize).take(LogPageSize)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(text = "复制日志", onClick = {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                        logger.log("管理员复制日志")
                        logs = logger.readLogs()
                        message = "日志已复制"
                    })
                    CompactButton(text = "清空日志", onClick = { showClearLogConfirm = true })
                }
                Divider(Modifier.padding(vertical = 4.dp))
                if (logs.isEmpty()) {
                    Text("暂无日志", style = AdminBodyStyle)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "共 ${pagedLogs.size} 条 · 第 ${currentPage + 1}/$totalPages 页",
                            modifier = Modifier.weight(1f),
                            style = AdminLogMetaStyle,
                        )
                        CompactButton(
                            text = "上一页",
                            enabled = currentPage > 0,
                            onClick = { logPage = currentPage - 1 },
                        )
                        CompactButton(
                            text = "下一页",
                            enabled = currentPage < totalPages - 1,
                            onClick = { logPage = currentPage + 1 },
                        )
                    }
                    pageLogs.forEach {
                        LogRow(entry = LogEntry.fromRaw(it))
                    }
                }
            }
        }
    }

    if (showClearLogConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogConfirm = false },
            title = { Text("清空日志") },
            text = { Text("确认清空本机 EasyPhoto 日志？") },
            confirmButton = {
                Button(
                    onClick = {
                        logger.clear()
                        logs = emptyList()
                        logPage = 0
                        message = "日志已清空"
                        showClearLogConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EasyPhotoDanger),
                ) {
                    AdminButtonText("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun AdminSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = AdminSectionTitleStyle)
        content()
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    leftText: String,
    rightText: String,
    leftSelected: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AdminBodyStyle)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onLeft,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (leftSelected) EasyPhotoPrimary else EasyPhotoSecondary,
                ),
            ) {
                AdminButtonText(leftText)
            }
            Button(
                onClick = onRight,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!leftSelected) EasyPhotoPrimary else EasyPhotoSecondary,
                ),
            ) {
                AdminButtonText(rightText)
            }
        }
    }
}

@Composable
private fun HomeBackgroundStatusRow(
    rearPath: String?,
    frontPath: String?,
    onSelectRear: () -> Unit,
    onSelectFront: () -> Unit,
    onClearRear: () -> Unit,
    onClearFront: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BackgroundStatusLine(
            title = "后置背景",
            enabled = !rearPath.isNullOrBlank(),
            onSelect = onSelectRear,
            onClear = onClearRear,
        )
        BackgroundStatusLine(
            title = "前置背景",
            enabled = !frontPath.isNullOrBlank(),
            onSelect = onSelectFront,
            onClear = onClearFront,
        )
    }
}

@Composable
private fun BackgroundStatusLine(
    title: String,
    enabled: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AdminSmallStyle)
            Text(if (enabled) "已设置缓存图片" else "未设置", style = AdminSmallStyle)
        }
        CompactButton(
            text = "选择",
            onClick = onSelect,
        )
        CompactButton(
            text = "清除",
            enabled = enabled,
            onClick = onClear,
        )
    }
}

@Composable
private fun FlashChoiceButton(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) EasyPhotoPrimary else EasyPhotoSecondary,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, style = AdminIconStyle)
            Text(text = label, style = AdminTinyStyle)
        }
    }
}

@Composable
private fun RowScope.SmallChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) EasyPhotoPrimary else EasyPhotoSecondary,
        ),
    ) {
        AdminButtonText(text)
    }
}

@Composable
private fun CompactButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(30.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Text(text = text, style = AdminTinyButtonStyle)
    }
}

@Composable
private fun AdminButtonText(text: String) {
    Text(text = text, style = AdminButtonStyle)
}

private val AdminHeadingStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)

private val AdminSectionTitleStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
)

private val AdminBodyStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
)

private val AdminSmallStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

private val AdminButtonStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
)

private val AdminTinyButtonStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 14.sp,
)

private val AdminIconStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 18.sp,
)

private val AdminTinyStyle = TextStyle(
    fontSize = 9.sp,
    lineHeight = 10.sp,
)

private val AdminLogLevelStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 12.sp,
)

private val AdminLogMetaStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 13.sp,
)

private val AdminLogMessageStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 13.sp,
)

private const val LogPageSize = 10

private fun Long.toReadableSize(): String {
    val mb = this / 1024.0 / 1024.0
    return "%.1f MB".format(mb)
}

private fun Context.hasCamera(lensFacing: Int): Boolean =
    runCatching {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.cameraIdList.any { cameraId ->
            manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == lensFacing
        }
    }.getOrDefault(false)

private fun Context.openHomeSettings() {
    val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        startActivity(homeSettingsIntent)
    }.onFailure {
        startActivity(fallbackIntent)
    }
}

private fun HomeBackgroundSlot.label(): String =
    when (this) {
        HomeBackgroundSlot.Rear -> "后置"
        HomeBackgroundSlot.Front -> "前置"
    }

@Composable
private fun LogRow(entry: LogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(entry.time, style = AdminLogMetaStyle, color = Color(0xFF475569))
            Text(
                entry.level,
                style = AdminLogLevelStyle,
                color = entry.levelColor,
            )
            Text(
                entry.module,
                modifier = Modifier.weight(1f),
                style = AdminLogMetaStyle,
                color = Color(0xFF2563EB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            entry.message,
            style = AdminLogMessageStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class LogEntry(
    val time: String,
    val level: String,
    val module: String,
    val message: String,
    val levelColor: Color,
) {
    companion object {
        fun fromRaw(raw: String): LogEntry {
            val time = raw.take(19)
            val message = raw.drop(21).ifBlank { raw }
            val isError = listOf("失败", "错误", "closed", "Not bound").any { message.contains(it, ignoreCase = true) }
            val module = when {
                message.contains("相机") || message.contains("拍照") -> "相机"
                message.contains("相册") || message.contains("照片") -> "相册"
                message.contains("管理员") -> "管理员"
                message.contains("App 启动") -> "应用"
                else -> "系统"
            }
            return LogEntry(
                time = time,
                level = if (isError) "ERROR" else "INFO",
                module = module,
                message = message,
                levelColor = if (isError) EasyPhotoDanger else EasyPhotoPrimary,
            )
        }
    }
}
