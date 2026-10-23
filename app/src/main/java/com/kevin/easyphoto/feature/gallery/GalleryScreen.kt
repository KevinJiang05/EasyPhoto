package com.kevin.easyphoto.feature.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kevin.easyphoto.R
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.media.EasyPhotoItem
import com.kevin.easyphoto.data.media.EasyPhotoMediaStore
import com.kevin.easyphoto.ui.theme.EasyPhotoDanger
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import com.kevin.easyphoto.ui.feedback.UserFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    mediaStore: EasyPhotoMediaStore,
    settings: EasyPhotoSettings,
    logger: EasyPhotoLogger,
    selectedUri: String?,
    onBackHome: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var photos by remember { mutableStateOf<List<EasyPhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var adminMode by remember { mutableStateOf(settings.adminModeEnabled) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val selectedUris = remember { mutableStateListOf<String>() }

    BackHandler { onBackHome() }

    LaunchedEffect(refreshKey) {
        adminMode = settings.adminModeEnabled
        isLoading = true
        logger.log("打开相册")
        mediaStore.queryPhotos(context)
            .onSuccess {
                photos = if (settings.newestFirst) it else it.reversed()
                error = false
            }
            .onFailure {
                logger.log("读取相册失败：${it.message.orEmpty()}")
                error = true
            }
        isLoading = false
    }

    LaunchedEffect(photos, selectedUri) {
        val index = photos.indexOfFirst { it.uri == selectedUri }
        if (index >= 0) {
            gridState.scrollToItem(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = {
                    UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                    onBackHome()
                },
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = "返回首页",
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = "照片",
                style = if (adminMode) GalleryAdminTitleStyle else MaterialTheme.typography.headlineMedium,
            )
            if (adminMode) {
                Button(onClick = onOpenSettings) {
                    Text("设置", style = GalleryAdminButtonStyle)
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
        }

        if (adminMode && photos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("已选择 ${selectedUris.size} 张", style = GalleryAdminBodyStyle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        selectedUris.clear()
                        selectedUris.addAll(photos.map { it.uri })
                    }) {
                        Text("全选", style = GalleryAdminButtonStyle)
                    }
                    Button(onClick = { selectedUris.clear() }) {
                        Text("取消", style = GalleryAdminButtonStyle)
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = selectedUris.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EasyPhotoDanger),
                    ) {
                        Text("删除", style = GalleryAdminButtonStyle)
                    }
                }
            }
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error -> GalleryMessage("暂时无法读取照片，请重试", null, null)
            GalleryEmptyState.shouldShowEmptyState(photos.size) -> GalleryMessage("还没有照片", "去拍照", onOpenCamera)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(settings.galleryColumnCount),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    GalleryPhotoTile(
                        photo = photo,
                        adminMode = adminMode,
                        selected = photo.uri in selectedUris,
                        onClick = {
                            if (adminMode) {
                                if (photo.uri in selectedUris) {
                                    selectedUris.remove(photo.uri)
                                } else {
                                    selectedUris.add(photo.uri)
                                }
                            } else {
                                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                                onOpenPhoto(photo.uri)
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("将删除 ${selectedUris.size} 张 EasyPhoto 照片。删除后 App 内不能恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        val deleting = selectedUris.toList()
                        scope.launch {
                            val deleted = mediaStore.deletePhotos(context, deleting).getOrDefault(0)
                            logger.log("管理员在相册删除照片：选择 ${deleting.size} 张，成功 $deleted 张")
                            selectedUris.clear()
                            refreshKey += 1
                        }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EasyPhotoDanger),
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun GalleryPhotoTile(
    photo: EasyPhotoItem,
    adminMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = "照片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (adminMode) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EasyPhotoPrimary.copy(alpha = 0.52f)),
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

private val GalleryAdminTitleStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)

private val GalleryAdminBodyStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
)

private val GalleryAdminButtonStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
)

@Composable
private fun GalleryMessage(
    text: String,
    actionText: String?,
    onAction: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = EasyPhotoPrimary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp, vertical = 24.dp),
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
