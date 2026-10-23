package com.kevin.easyphoto.feature.camera

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kevin.easyphoto.R
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.ui.feedback.UserFeedback
import com.kevin.easyphoto.ui.theme.EasyPhotoPrimary
import com.kevin.easyphoto.ui.theme.EasyPhotoSecondary

@Composable
fun CaptureResultScreen(
    photoUri: Uri,
    settings: EasyPhotoSettings,
    onTakeAnother: () -> Unit,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
) {
    val view = LocalView.current
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = photoUri,
            contentDescription = "刚拍的照片",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(28.dp))
        ResultButton(
            text = "再拍一张",
            iconRes = R.drawable.ic_camera,
            color = EasyPhotoPrimary,
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onTakeAnother()
            },
        )
        Spacer(Modifier.height(18.dp))
        ResultButton(
            text = "查看照片",
            iconRes = R.drawable.ic_gallery,
            color = EasyPhotoSecondary,
            onClick = {
                UserFeedback.tap(view, settings.hapticFeedbackEnabled)
                onOpenGallery()
            },
        )
    }
}

@Composable
private fun ResultButton(
    text: String,
    iconRes: Int,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 26.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
