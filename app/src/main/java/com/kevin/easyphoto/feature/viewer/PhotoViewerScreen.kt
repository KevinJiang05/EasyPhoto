package com.kevin.easyphoto.feature.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kevin.easyphoto.R
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.media.EasyPhotoItem
import com.kevin.easyphoto.data.media.EasyPhotoMediaStore
import com.kevin.easyphoto.ui.feedback.UserFeedback
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    initialUri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<EasyPhotoItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        mediaStore.queryPhotos(context).onSuccess {
            photos = it
        }
        loaded = true
    }

    val initialIndex = photos.indexOfFirst { it.uri == initialUri }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size.coerceAtLeast(1) }

    LaunchedEffect(photos, initialUri) {
        val targetIndex = photos.indexOfFirst { it.uri == initialUri }
        if (targetIndex >= 0) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (loaded && photos.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                key(photos[index].uri) {
                    ZoomablePhoto(
                        uri = photos[index].uri,
                        zoomEnabled = settings.viewerZoomEnabled,
                    )
                }
            }
        } else {
            Text(
                text = if (loaded) "暂时无法读取照片" else "正在打开照片",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        ViewerIconButton(
            iconRes = R.drawable.ic_gallery,
            contentDescription = "返回相册",
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        )
        if (settings.viewerArrowButtonsEnabled && photos.size > 1) {
            ViewerIconButton(
                iconRes = R.drawable.ic_arrow_left,
                contentDescription = "上一张照片",
                enabled = pagerState.currentPage > 0,
                onClick = {
                    UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
            ViewerIconButton(
                iconRes = R.drawable.ic_arrow_right,
                contentDescription = "下一张照片",
                enabled = pagerState.currentPage < photos.lastIndex,
                onClick = {
                    UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ZoomablePhoto(
    uri: String,
    zoomEnabled: Boolean,
) {
    var scale by remember(uri, zoomEnabled) { mutableFloatStateOf(1f) }
    var offset by remember(uri, zoomEnabled) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(uri) { mutableStateOf(IntSize.Zero) }

    val gestureModifier = if (zoomEnabled) {
        Modifier.pointerInput(uri) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val pressedPointers = event.changes.count { it.pressed }
                    if (pressedPointers >= 2 || scale > 1f) {
                        val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                        val pan = event.calculatePan()
                        val maxOffsetX = containerSize.width * (nextScale - 1f) / 2f
                        val maxOffsetY = containerSize.height * (nextScale - 1f) / 2f

                        scale = nextScale
                        offset = if (nextScale <= 1f) {
                            Offset.Zero
                        } else {
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY),
                            )
                        }
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerSize = it },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "照片",
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ViewerIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = EasyPhotoPrimary,
            disabledContainerColor = Color.White.copy(alpha = 0.45f),
            disabledContentColor = EasyPhotoPrimary.copy(alpha = 0.35f),
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(42.dp),
        )
    }
}
