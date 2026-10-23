package com.kevin.easyphoto.feature.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.AspectRatio
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.kevin.easyphoto.R
import com.kevin.easyphoto.data.admin.AfterCaptureAction
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.admin.FlashMode
import com.kevin.easyphoto.data.feishu.FeishuPhotoScheduler
import com.kevin.easyphoto.data.media.EasyPhotoMediaStore
import com.kevin.easyphoto.ui.theme.EasyPhotoDanger
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import com.kevin.easyphoto.ui.feedback.UserFeedback
import com.kevin.easyphoto.util.StoragePermissionPolicy
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    logger: EasyPhotoLogger,
    lensFacing: Int,
    onBackHome: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onOpenGallery: (Uri) -> Unit,
    onReturnToCamera: () -> Unit,
) {
    val context = LocalContext.current
    val requiredPermissions = remember { StoragePermissionPolicy.requiredPermissions(Build.VERSION.SDK_INT) }
    var hasPermissions by remember { mutableStateOf(requiredPermissions.all { context.hasPermission(it) }) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermissions = requiredPermissions.all { result[it] == true || context.hasPermission(it) }
        permanentlyDenied = !hasPermissions
    }

    BackHandler { onBackHome() }

    if (!hasPermissions) {
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onAllow = {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
            onBackHome = onBackHome,
        )
    } else {
        CameraPreviewScreen(
            mediaStore = mediaStore,
            settings = settings,
            logger = logger,
            lensFacing = lensFacing,
            onBackHome = onBackHome,
            onCaptured = onCaptured,
            onOpenGallery = onOpenGallery,
            onReturnToCamera = onReturnToCamera,
        )
    }
}

@Composable
private fun PermissionScreen(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (permanentlyDenied) "请在系统设置中允许相机权限" else "需要使用相机来拍照",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(28.dp))
        if (permanentlyDenied) {
            Button(onClick = onOpenSettings) {
                Text("打开设置", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Button(onClick = onAllow) {
                Text("允许相机", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(16.dp))
        }
        TopIconButton(
            iconRes = R.drawable.ic_home,
            contentDescription = "返回首页",
            onClick = onBackHome,
        )
    }
}

@Composable
@androidx.annotation.OptIn(markerClass = [ExperimentalZeroShutterLag::class])
private fun CameraPreviewScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    logger: EasyPhotoLogger,
    lensFacing: Int,
    onBackHome: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onOpenGallery: (Uri) -> Unit,
    onReturnToCamera: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    val feishuPhotoScheduler = remember { FeishuPhotoScheduler(context.applicationContext) }
    val mirrorFrontCamera =
        lensFacing == CameraSelector.LENS_FACING_FRONT && settings.frontCameraMirrorEnabled
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var preparedOutputOptions by remember { mutableStateOf<ImageCapture.OutputFileOptions?>(null) }
    var showShutterFlash by remember { mutableStateOf(false) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("正在打开相机") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lensFacing, mirrorFrontCamera) {
        previewView.scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT && !mirrorFrontCamera) {
            -1f
        } else {
            1f
        }
        val provider = ProcessCameraProvider.getInstance(context).get()
        runCatching {
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            imageCapture = capture
            camera = boundCamera
            val shouldEnableTorch = settings.flashMode == FlashMode.On && boundCamera.cameraInfo.hasFlashUnit()
            if (shouldEnableTorch) {
                boundCamera.cameraControl.enableTorch(true)
                isTorchOn = true
            }
            status = ""
            logger.log("${lensFacing.label()}相机打开成功")
        }.onFailure {
            logger.log("${lensFacing.label()}相机打开失败：${it.message.orEmpty()}")
            error = "无法打开${lensFacing.label()}相机，请返回首页"
        }
    }

    LaunchedEffect(imageCapture) {
        if (imageCapture != null && preparedOutputOptions == null) {
            try {
                preparedOutputOptions = mediaStore.createOutputOptions(
                    context = context,
                    reversedHorizontal = mirrorFrontCamera,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.log("准备照片保存位置失败：${exception.message.orEmpty()}")
                error = "无法准备照片保存位置，请重试"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    LaunchedEffect(showShutterFlash) {
        if (showShutterFlash) {
            delay(80)
            showShutterFlash = false
        }
    }

    if (error != null) {
        ErrorScreen(message = error.orEmpty(), onBackHome = onBackHome)
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            update = {
                imageCapture?.targetRotation = it.display?.rotation ?: Surface.ROTATION_0
            },
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopIconButton(
                iconRes = R.drawable.ic_home,
                contentDescription = "返回首页",
                onClick = {
                    UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                    onBackHome()
                },
            )
            val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
            if (hasFlash) {
                TopIconButton(
                    iconRes = if (isTorchOn) {
                        R.drawable.ic_flashlight_on
                    } else {
                        R.drawable.ic_flashlight_off
                    },
                    contentDescription = if (isTorchOn) "关闭闪光灯" else "打开闪光灯",
                    active = isTorchOn,
                    onClick = {
                        UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                        isTorchOn = !isTorchOn
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    },
                )
            }
        }
        if (showShutterFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.42f)),
            )
        }
        if (status.isNotBlank()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = status,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        Button(
            onClick = {
                val capture = imageCapture ?: return@Button
                val options = preparedOutputOptions ?: return@Button
                isSaving = true
                preparedOutputOptions = null
                capture.takePicture(
                    options,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val uri = outputFileResults.savedUri
                            if (uri == null) {
                                isSaving = false
                                logger.log("${lensFacing.label()}拍照失败：没有返回照片地址")
                                error = "照片保存失败，请重试"
                                return
                            }
                            scope.launch {
                                runCatching { mediaStore.markSaved(context, uri) }
                                    .onSuccess {
                                        logger.log("${lensFacing.label()}拍照保存成功：$uri")
                                        if (settings.feishuNotificationsEnabled) {
                                            runCatching { feishuPhotoScheduler.enqueue(uri.toString()) }
                                                .onFailure {
                                                    logger.log("飞书通知排队失败：${it.message.orEmpty()}")
                                                }
                                        }
                                        lastCapturedUri = uri
                                        UserFeedback.captureSuccess(
                                            view = view,
                                            hapticEnabled = settings.hapticFeedbackEnabled,
                                            soundEnabled = settings.soundFeedbackEnabled,
                                        )
                                        isSaving = false
                                        if (settings.afterCaptureAction == AfterCaptureAction.ReturnToCamera) {
                                            onReturnToCamera()
                                        } else {
                                            onCaptured(uri)
                                        }
                                    }
                                    .onFailure { throwable ->
                                        isSaving = false
                                        logger.log(
                                            "${lensFacing.label()}拍照失败：${throwable.message.orEmpty()}",
                                        )
                                        error = if (throwable is IOException) {
                                            "存储空间不足，请联系家人"
                                        } else {
                                            "照片保存失败，请重试"
                                        }
                                    }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isSaving = false
                            logger.log("${lensFacing.label()}拍照失败：${exception.message.orEmpty()}")
                            error = "照片保存失败，请重试"
                        }
                    },
                )
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                showShutterFlash = true
                scope.launch {
                    try {
                        preparedOutputOptions = mediaStore.createOutputOptions(
                            context = context,
                            reversedHorizontal = mirrorFrontCamera,
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        logger.log("准备下一张照片失败：${exception.message.orEmpty()}")
                        error = "无法准备照片保存位置，请重试"
                    }
                }
            },
            enabled = !isSaving && imageCapture != null && preparedOutputOptions != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .size(96.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EasyPhotoPrimary),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .border(5.dp, EasyPhotoPrimary, CircleShape),
            )
        }
        lastCapturedUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "打开刚拍的照片",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 22.dp, bottom = 44.dp)
                    .size(88.dp)
                    .border(4.dp, Color.White, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                        onOpenGallery(uri)
                    },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun TopIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFFFFD54F) else Color.White,
            contentColor = EasyPhotoPrimary,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun ErrorScreen(message: String, onBackHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = EasyPhotoDanger, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        TopIconButton(
            iconRes = R.drawable.ic_home,
            contentDescription = "返回首页",
            onClick = onBackHome,
        )
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Int.label(): String = if (this == CameraSelector.LENS_FACING_FRONT) "前置" else "后置"
