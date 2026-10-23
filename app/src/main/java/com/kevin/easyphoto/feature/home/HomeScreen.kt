package com.kevin.easyphoto.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.kevin.easyphoto.data.admin.AdminPinPolicy
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.media.EasyPhotoItem
import com.kevin.easyphoto.data.media.EasyPhotoMediaStore
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import com.kevin.easyphoto.ui.theme.EasyPhotoSecondary
import com.kevin.easyphoto.ui.feedback.UserFeedback
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    logger: EasyPhotoLogger,
    onOpenRearCamera: () -> Unit,
    onOpenFrontCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    BackHandler(enabled = true) {
        // Home can later act as a launcher surface; keep accidental Back presses on the home screen.
    }

    val context = LocalContext.current
    val view = LocalView.current
    val appVersionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
    }
    var recentPhotos by remember { mutableStateOf<List<EasyPhotoItem>>(emptyList()) }
    var galleryPreviewCount by remember { mutableStateOf(settings.galleryPreviewCount) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }
    var adminConfirmation by remember { mutableStateOf("") }
    var adminError by remember { mutableStateOf<String?>(null) }
    var adminBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var rearBackgroundPath by remember { mutableStateOf(settings.rearHomeBackgroundPath) }
    var frontBackgroundPath by remember { mutableStateOf(settings.frontHomeBackgroundPath) }
    var refreshKey by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshKey, galleryPreviewCount) {
        galleryPreviewCount = settings.galleryPreviewCount
        rearBackgroundPath = settings.rearHomeBackgroundPath
        frontBackgroundPath = settings.frontHomeBackgroundPath
        mediaStore.queryPhotos(context)
            .onSuccess { recentPhotos = it.take(galleryPreviewCount) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CameraHomePanel(
            title = "后置拍摄",
            hint = "拍别人和风景",
            color = Color(0xFF1F2937),
            backgroundPath = rearBackgroundPath,
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onOpenRearCamera()
            },
            modifier = Modifier.weight(1f),
        )
        CameraHomePanel(
            title = "前置拍摄",
            hint = "拍自己",
            color = Color(0xFF3F3F46),
            backgroundPath = frontBackgroundPath,
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onOpenFrontCamera()
            },
            modifier = Modifier.weight(1f),
        )
        GalleryHomePanel(
            photos = recentPhotos,
            previewCount = galleryPreviewCount,
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onOpenGallery()
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "EasyPhoto v$appVersionName  KevinJiang1018@gmail.com",
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val heldLongEnough = withTimeoutOrNull(4_000L) {
                                tryAwaitRelease()
                                false
                            } ?: true
                            if (heldLongEnough) {
                                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                                showAdminDialog = true
                            }
                        },
                    )
                },
            color = Color(0xFF6B7280),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }

    if (showAdminDialog) {
        val needsAdminPin = !settings.hasAdminPin
        AlertDialog(
            onDismissRequest = {
                if (!adminBusy) {
                    showAdminDialog = false
                    adminPassword = ""
                    adminConfirmation = ""
                    adminError = null
                }
            },
            title = { Text(if (needsAdminPin) "设置管理员 PIN" else "管理员模式") },
            text = {
                Column {
                    if (needsAdminPin) {
                        Text("请由家属设置 6 位数字 PIN，以后进入管理员模式时使用。")
                    }
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            adminError = null
                        },
                        label = { Text("6 位数字 PIN") },
                        singleLine = true,
                        enabled = !adminBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = adminError != null,
                    )
                    if (needsAdminPin) {
                        OutlinedTextField(
                            value = adminConfirmation,
                            onValueChange = {
                                adminConfirmation = it
                                adminError = null
                            },
                            label = { Text("再次输入 PIN") },
                            singleLine = true,
                            enabled = !adminBusy,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = adminError != null,
                        )
                    }
                    adminError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !adminBusy,
                    onClick = {
                        if (!AdminPinPolicy.isValid(adminPassword)) {
                            adminError = "请输入 6 位数字 PIN"
                        } else if (needsAdminPin && adminPassword != adminConfirmation) {
                            adminError = "两次输入的 PIN 不一致"
                        } else {
                            adminBusy = true
                            scope.launch {
                                val result = runCatching {
                                    withContext(Dispatchers.Default) {
                                        if (needsAdminPin) {
                                            settings.setInitialAdminPin(adminPassword)
                                            true
                                        } else {
                                            settings.verifyAdminPin(adminPassword)
                                        }
                                    }
                                }
                                adminBusy = false
                                if (result.getOrNull() == true) {
                                    logger.log("进入管理员模式")
                                    settings.adminModeEnabled = true
                                    showAdminDialog = false
                                    adminPassword = ""
                                    adminConfirmation = ""
                                    adminError = null
                                    onOpenAdmin()
                                } else {
                                    adminError = if (result.isFailure) "无法保存或验证 PIN，请重试" else "PIN 不正确"
                                }
                            }
                        }
                    },
                ) {
                    Text(if (adminBusy) "请稍候" else if (needsAdminPin) "设置并进入" else "进入")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !adminBusy,
                    onClick = {
                        showAdminDialog = false
                        adminPassword = ""
                        adminConfirmation = ""
                        adminError = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun CameraHomePanel(
    title: String,
    hint: String,
    color: Color,
    backgroundPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .clickable(onClick = onClick),
    ) {
        val backgroundFile = backgroundPath?.let(::File)?.takeIf { it.exists() }
        if (backgroundFile != null) {
            AsyncImage(
                model = backgroundFile,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.96f), Color.Black.copy(alpha = 0.50f)),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = hint,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun GalleryHomePanel(
    photos: List<EasyPhotoItem>,
    previewCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(EasyPhotoSecondary)
            .clickable(onClick = onClick),
    ) {
        if (photos.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                photos.chunked(5).take(3).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        row.forEach { photo ->
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = "照片",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        repeat(5 - row.size) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.18f)),
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.68f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
        ) {
            Text(
                text = "看照片",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "最近 $previewCount 张",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
